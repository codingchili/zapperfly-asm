package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.*;
import io.vertx.core.Future;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;
import com.codingchili.core.storage.*;

import static com.codingchili.zapperflyasm.model.ApiRequest.*;
import static com.codingchili.zapperflyasm.model.BuildJob.START;
import static com.codingchili.zapperflyasm.model.Status.*;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - distributes across the cluster.
 */
public class DefaultJobManager implements JobManager {
    private ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private static final int INSTANCE_TIMEOUT = 4000;
    private static final String BUILD = "build";
    private static final String PROGRESS = "progress";
    private static final String INSTANCE = "instance";
    private AsyncStorage<BuildJob> jobs;
    private AsyncStorage<LogEvent> logs;
    private AsyncStorage<InstanceInfo> instances;
    private VersionControlSystem vcs;
    private ZapperConfig config = ZapperConfig.get();
    private InstanceInfo instance = new InstanceInfo();
    private BuildExecutor executor;
    private Logger logger;
    private CoreContext core;

    /**
     * Creates a new clustered job manager.
     *
     * @param core the core on which the manager is to be run.
     * @param jobs a potentially clustered store of jobs to execute.
     * @param logs a potentially clulstered store to store job logs.
     */
    public DefaultJobManager(CoreContext core,
                             AsyncStorage<BuildJob> jobs,
                             AsyncStorage<LogEvent> logs,
                             AsyncStorage<InstanceInfo> instances) {

        this.vcs = new GitExecutor(core);
        this.executor = new ProcessBuilderExecutor(core);
        this.jobs = jobs;
        this.logs = logs;
        this.core = core;
        this.logger = core.logger(getClass());
        this.instances = instances;

        // save this instance to the cluster.
        save(instance);

        // start polling timers.
        timers();
    }

    private void timers() {
        // cannot run this on the event loop thread - because cluster convergence
        // blocks the vertx thread. so when a single instance goes offline
        // all other instances will be falsely detected as offline.
        thread.scheduleWithFixedDelay(() -> {
            try {
                instance.calculateSystemLoad();
                save(instance);
            } catch (Throwable e) {
                throw new CoreRuntimeException(e.getMessage());
            }
        }, 0, INSTANCE_TIMEOUT / 2, TimeUnit.MILLISECONDS);

        core.periodic(() -> 500, "pollQueue", (id) -> poll());
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
            jobs.query(PROGRESS).equalTo(Status.QUEUED)
                    .order(SortOrder.DESCENDING)
                    .orderBy(START)
                    .pageSize(1)
                    .page(0)
                    .execute(query -> {
                        if (query.succeeded() && query.result().size() > 0) {
                            BuildJob job = query.result().iterator().next();
                            begin(job);

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
                        } else if (query.failed()) {
                            logger.onError(query.cause());
                        }
                    });
        }
    }

    private void begin(BuildJob job) {
        instance.setBuilds(instance.getBuilds() + 1);
        job.setSaver(this::save);
        job.setLogger(this::log);
        job.setInstance(config.getInstanceName());
        job.log("Build starting on executor '" + instance.getId() + "' ..");
        save(job);
    }

    private void complete() {
        instance.setBuilds(instance.getBuilds() - 1);
        save(instance);
    }

    @Override
    public Future<BuildJob> submit(BuildConfiguration config) {
        Future<BuildJob> future = Future.future();
        BuildJob job = new BuildJob();
        job.setConfig(config);
        log(job, "Build " + job.getId() + " queued.");

        // add the job to the queue.
        jobs.put(job, (put) -> {
            if (put.succeeded()) {
                future.complete(job);
            } else {
                future.fail(put.cause());
            }
        });
        return future;
    }

    private void save(BuildJob job) {
        jobs.put(job, (done) -> {
            if (done.failed()) {
                logger.onError(done.cause());
            }
        });
    }

    private void log(BuildJob job, String line) {
        if (line != null && !line.isEmpty()) {
            logs.put(new LogEvent()
                    .setBuild(job.getId())
                    .setLine(line), done -> {
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
        jobs.query(INSTANCE).equalTo(instance.getId())
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
        jobs.get(buildId, future);
        return future;
    }

    @Override
    public Future<Collection<BuildJob>> getAll() {
        Future<Collection<BuildJob>> future = Future.future();
        jobs.query(ID_BUILD).matches(".*")
                .pageSize(24).page(0)
                .orderBy(START)
                .order(SortOrder.DESCENDING)
                .execute(future);
        return future;
    }

    @Override
    public Future<Collection<LogEvent>> getLog(String buildId, Long time) {
        Future<Collection<LogEvent>> future = Future.future();
        QueryBuilder<LogEvent> query = logs.query(BUILD).equalTo(buildId)
                .and(ID_TIME).between(time + 1, Long.MAX_VALUE)
                .orderBy(ID_TIME)
                .order(SortOrder.ASCENDING);

        query.execute(done -> {
            if (done.succeeded()) {
                future.complete(done.result().stream()
                        .map(event -> event.setBuild(null).setId(null)).collect(Collectors.toList()));
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

        Future<Void> clearLog = Future.future();
        logs.clear(clearLog);

        Future<Void> clearBuilds = Future.future();
        jobs.query(PROGRESS).in(FAILED, DONE, CANCELLED).execute(query -> {
            if (query.succeeded()) {
                AtomicInteger count = new AtomicInteger(query.result().size());

                // nothing to remove - make sure to complete.
                if (count.get() == 0) {
                    clearBuilds.complete();
                }

                // try and remove all query results.
                query.result().forEach(job -> {
                    jobs.remove(job.getId(), (removed) -> {
                        if (removed.failed()) {
                            clearBuilds.tryFail(removed.cause());
                        } else {
                            if (count.decrementAndGet() == 0) {
                                clearBuilds.complete();
                            }
                        }
                    });
                });
            } else {
                clearBuilds.fail(query.cause());
            }
        });

        CompositeFuture.all(clearLog, clearBuilds).setHandler(done -> {
            if (done.succeeded()) {
                future.complete();
            } else {
                future.fail(done.cause());
            }
        });
        return future;
    }

    /**
     * @param vcs the version control system to use.
     */
    public void setVCSProvider(VersionControlSystem vcs) {
        this.vcs = vcs;
    }

    /**
     * @param executor the build executor to use.
     */
    public void setBuildExecutor(BuildExecutor executor) {
        this.executor = executor;
    }
}
