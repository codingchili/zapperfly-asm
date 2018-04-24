package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;
import com.codingchili.core.storage.AsyncStorage;
import com.codingchili.core.storage.SortOrder;

import static com.codingchili.zapperflyasm.model.ApiRequest.ID_BUILD;
import static com.codingchili.zapperflyasm.model.BuildJob.START;
import static com.codingchili.zapperflyasm.model.HazelJobQueue.POLL_WAIT;
import static com.codingchili.zapperflyasm.model.Status.*;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - distributes across the cluster.
 */
public class DefaultBuildManager implements BuildManager {
    private ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private static final int INSTANCE_TIMEOUT = 4000;
    private static final String PROGRESS = "progress";
    private static final String INSTANCE = "instance";
    private ZapperConfig config = ZapperConfig.get();
    private InstanceInfo instance = new InstanceInfo();
    private VersionControlSystem vcs;
    private AsyncStorage<InstanceInfo> instances;
    private AsyncStorage<BuildJob> builds;
    private BuildExecutor executor;
    private JobQueue queue;
    private LogStore logs;
    private CoreContext core;
    private Logger logger;

    /**
     * Creates a new clustered job manager.
     *
     * @param core the core context.
     */
    public DefaultBuildManager(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
    }

    public void start() {
        // cannot run this on the event loop thread - because cluster convergence
        // blocks the vertx thread. so when a single instance goes offline
        // all other instances will be falsely detected as offline.
        thread.scheduleWithFixedDelay(() -> {
            try {
                instance.calculateSystemLoad();
                save(instance);
            } catch (Throwable e) {
                logger.onError(e);
            }
        }, 0, INSTANCE_TIMEOUT / 2, TimeUnit.MILLISECONDS);

        core.periodic(() -> POLL_WAIT, "jobPoller", (id) -> poll());
    }

    private void save(InstanceInfo instance) {
        instance.setUpdated(System.currentTimeMillis());
        instances.put(instance, done -> {
            if (done.failed()) {
                logger.onError(done.cause());
            }
        });
    }

    /**
     * check if there are any jobs in the queue.
     */
    private void poll() {
        if (instance.getBuilds() < instance.getCapacity()) {
            queue.poll().setHandler(done -> {
                if (done.succeeded() && done.result() != null) {
                    BuildJob job = done.result();
                    job.setProgress(CLONING);
                    job.setInstance(config.getInstanceName());

                    builds.put(job, (save) -> {
                        if (save.succeeded()) {
                            run(job);
                        } else {
                            logger.onError(save.cause());
                        }
                    });
                } else {
                    if (done.failed()) {
                        logger.onError(done.cause());
                    }
                }
            });
        }
    }

    private void run(BuildJob job) {
        instance.setBuilds(instance.getBuilds() + 1);
        job.setSaver(this::save);
        job.setLogger(this::log);
        job.log("Build starting on executor '" + instance.getId() + "' ..");

        // clone the repository and the branch.
        vcs.clone(job).setHandler(clone -> {
            if (clone.succeeded()) {
                // cloned ok - start building.
                executor.build(job).setHandler(
                        (executed) -> handleCompleted(executed, job));
            } else {
                complete();
                throw new CoreRuntimeException(clone.cause().getMessage());
            }
        });
    }

    private void complete() {
        instance.setBuilds(instance.getBuilds() - 1);
        save(instance);
        poll();
    }

    @Override
    public Future<BuildJob> submit(BuildConfiguration config) {
        Future<BuildJob> future = Future.future();
        BuildJob job = new BuildJob();
        job.setConfig(config);
        log(job, "Build " + job.getId() + " queued.");

        // add the job to the queue.
        queue.submit(job).setHandler(done -> {
            if (done.succeeded()) {
                future.complete(job);
            } else {
                future.fail(done.cause());
            }
        });
        return future;
    }

    private void save(BuildJob job) {
        builds.put(job, (done) -> {
            if (done.failed()) {
                logger.onError(done.cause());
            }
        });
    }

    private void log(BuildJob job, String line) {
        if (line != null && !line.isEmpty()) {
            logs.add(job.getId(), new LogEvent(line)).setHandler(done -> {
                if (done.failed()) {
                    logger.onError(done.cause());
                }
            });
        }
    }

    private void handleCompleted(AsyncResult<Void> build, BuildJob job) {
        complete();
        if (build.succeeded() && job.getConfig().isAutoclean()) {
            vcs.delete(job);
        }
    }

    @Override
    public Future<Collection<InstanceInfo>> instances() {
        Future<Collection<InstanceInfo>> future = Future.future();
        instances.values(values -> {
            if (values.succeeded()) {
                future.complete(values.result().stream().peek(i -> {
                    if (i.getUpdated() < System.currentTimeMillis() - INSTANCE_TIMEOUT) {
                        if (i.isOnline()) {
                            i.setOnline(false);
                            failAllBuildsOnInstance(i);
                            save(i);
                        }
                    }
                }).sorted(Comparator.comparing(InstanceInfo::getId))
                        .collect(Collectors.toList()));
            } else {
                future.fail(values.cause());
            }
        });
        return future;
    }

    private void failAllBuildsOnInstance(InstanceInfo instance) {
        builds.query(INSTANCE).equalTo(instance.getId())
                .and(PROGRESS).in(Status.CLONING, Status.BUILDING)
                .execute(query -> {
                    if (query.succeeded()) {
                        query.result().forEach(job -> {
                            job.setProgress(FAILED);
                            log(job, String.format("'%s' has gone offline - build failed.", job.getInstance()));
                            save(job);
                        });
                    } else {
                        logger.onError(query.cause());
                    }
                });
    }

    @Override
    public Future<Void> cancel(BuildJob job) {
        throw new UnsupportedOperationException("Cancelling builds not implemented yet.");
    }

    @Override
    public Future<Void> delete(BuildJob job) {
        Future<Void> future = Future.future();
        vcs.delete(job).setHandler(future);
        return future;
    }

    @Override
    public Future<List<String>> artifacts(BuildJob job) {
        return vcs.artifacts(job);
    }

    @Override
    public Future<BuildJob> getBuild(String buildId) {
        Future<BuildJob> future = Future.future();
        builds.get(buildId, future);
        return future;
    }

    @Override
    public Future<Collection<BuildJob>> queued() {
        Future<Collection<BuildJob>> future = Future.future();
        queue.values().setHandler(values -> {
            if (values.succeeded()) {
                future.complete(values.result());
            } else {
                future.fail(values.cause());
            }
        });
        return future;
    }

    @Override
    public Future<Collection<BuildJob>> history() {
        Future<Collection<BuildJob>> future = Future.future();
        builds.query(ID_BUILD).matches(".*")
                .pageSize(24).page(0)
                .orderBy(START)
                .order(SortOrder.DESCENDING)
                .execute(query -> {
                    if (query.succeeded()) {
                        future.complete(query.result());
                    } else {
                        future.fail(query.cause());
                    }
                });
        return future;
    }

    @Override
    public Future<Collection<LogEvent>> getLog(String buildId, Long time) {
        Future<Collection<LogEvent>> future = Future.future();

        logs.retrieve(buildId, time).setHandler(done -> {
            if (done.succeeded()) {
                future.complete(done.result());
            } else {
                logger.onError(done.cause());
                future.fail(done.cause());
            }
        });
        return future;
    }

    @Override
    public Future<Void> clear() {
        Future<Void> future = Future.future();
        builds.query(PROGRESS).in(FAILED, DONE, CANCELLED).execute(query -> {
            if (query.succeeded()) {
                AtomicInteger count = new AtomicInteger(query.result().size());

                // nothing to remove - make sure to complete.
                if (count.get() == 0) {
                    future.complete();
                }

                // try and remove all query results.
                query.result().forEach(job -> {
                    logs.clear(job.getId()).setHandler(logRemove -> {
                        builds.remove(job.getId(), (removed) -> {
                            if (removed.failed()) {
                                future.tryFail(removed.cause());
                            } else {
                                if (count.decrementAndGet() == 0) {
                                    future.complete();
                                }
                            }
                        });

                        if (logRemove.failed()) {
                            logger.onError(logRemove.cause());
                        }
                    });
                });
            } else {
                future.fail(query.cause());
            }
        });
        return future;
    }

    public void setVcs(VersionControlSystem vcs) {
        this.vcs = vcs;
    }

    public void setInstances(AsyncStorage<InstanceInfo> instances) {
        this.instances = instances;

        // save this instance to the cluster.
        save(instance);
    }

    public void setBuilds(AsyncStorage<BuildJob> builds) {
        this.builds = builds;
    }

    public void setQueue(JobQueue queue) {
        this.queue = queue;
    }

    public void setLogs(LogStore logs) {
        this.logs = logs;
    }

    public void setExecutor(BuildExecutor executor) {
        this.executor = executor;
    }
}
