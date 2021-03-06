package com.codingchili.zapperflyasm.building;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.logging.LogEvent;
import com.codingchili.zapperflyasm.logging.LogStore;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.scheduling.JobQueue;
import com.codingchili.zapperflyasm.vcs.VersionControlSystem;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.codingchili.core.configuration.CoreStrings;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.files.Configurations;
import com.codingchili.core.logging.Logger;
import com.codingchili.core.protocol.Serializer;
import com.codingchili.core.storage.*;

import static com.codingchili.zapperflyasm.model.ApiRequest.ID;
import static com.codingchili.zapperflyasm.model.BuildJob.START;
import static com.codingchili.zapperflyasm.model.Status.*;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - distributes across the cluster.
 */
public class DefaultBuildManager implements BuildManager {
    private static final int POLL_WAIT = 2000;
    private static final int INSTANCE_TIMEOUT = 5000;
    private static final int INSTANCE_UPDATE = 1000;
    private static final String CONFIG_REPOSITORY = "config.repositoryName";
    private static final String CONFIG_BRANCH = "config.branch";
    private static final String BUS_CANCEL = "cancel";
    private ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private static final String PROGRESS = "progress";
    private static final String INSTANCE = "instance";
    private InstanceInfo instance = InstanceInfo.get();
    private ZapperContext core;
    private AsyncStorage<InstanceInfo> instances;
    private AsyncStorage<BuildJob> builds;
    private VersionControlSystem vcs;
    private BuildExecutor executor;
    private JobQueue queue;
    private LogStore logs;
    private Logger logger;

    @Override
    public void init(ZapperContext core) {
        this.logger = core.logger(getClass());
        this.core = core;

        vcs = core.getVcs();
        executor = core.getExecutor();
        logs = core.getLogStore();

        core.bus().consumer(BUS_CANCEL, msg -> {
           BuildJob job = Serializer.unpack((JsonObject) msg.body(), BuildJob.class);
           executor.cancel(job.getId());
        });

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
        }, 0, INSTANCE_UPDATE, TimeUnit.MILLISECONDS);

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
            queue.poll(DefaultBuildManager.POLL_WAIT).setHandler(done -> {
                if (done.succeeded() && done.result() != null) {
                    BuildJob job = done.result();
                    job.setProgress(CLONING);
                    job.setInstance(ZapperConfig.getEnvironment().getInstanceName());

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

    private void run(BuildJob running) {
        instance.setBuilds(instance.getBuilds() + 1);
        running.setSaver(this::save);
        running.setLogger((job, line) -> logs.add(job.getId(), line));

        logs.onBuildStarted(running);

        if (requiresVCSCheckout(running)) {
            // clone the repository and the branch.
            vcs.clone(running).setHandler(clone -> {
                if (clone.succeeded()) {
                    // cloned ok - start building.
                    executor.build(running).setHandler((executed) -> handleCompleted(executed, running));
                } else {
                    complete();
                    throw new CoreRuntimeException(clone.cause().getMessage());
                }
            });
        } else {
            executor.build(running).setHandler((executed) -> handleCompleted(executed, running));
        }
    }

    private boolean requiresVCSCheckout(BuildJob job) {
        return !job.getConfig().getBranch().isEmpty();
    }

    private void complete() {
        instance.setBuilds(instance.getBuilds() - 1);
        save(instance);
        poll();
    }

    @Override
    public Future<BuildJob> submit(BuildJob job) {
        Future<BuildJob> future = Future.future();

        // add the job to the queue.
        queue.submit(job).setHandler(done -> {
            if (done.succeeded()) {
                logs.onBuildQueued(job);
                future.complete(job);
            } else {
                future.fail(done.cause());
            }
        });
        return future;
    }

    private Future<Void> save(BuildJob job) {
        Future<Void> future = Future.future();
        builds.put(job, (done) -> {
            if (done.failed()) {
                logger.onError(done.cause());
                future.fail(done.cause());
            } else {
                future.complete();
            }
        });
        return future;
    }

    private void handleCompleted(AsyncResult<Void> build, BuildJob job) {
        complete();
        logs.onBuildComplete(job);
        if (build.succeeded() && job.getConfig().isAutoclean()) {
            vcs.delete(job);
        }
    }

    @Override
    public Future<List<InstanceInfo>> instances() {
        Future<List<InstanceInfo>> future = Future.future();
        instances.values(values -> {
            if (values.succeeded()) {
                future.complete(values.result().stream().peek(instance -> {
                    if (instance.getUpdated() < System.currentTimeMillis() - INSTANCE_TIMEOUT) {
                        if (instance.isOnline()) {
                            instance.setOnline(false);
                            logs.onBuildExecutorOffline(instance);
                            failAllBuildsOnInstance(instance);
                            save(instance);
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
                            logs.onBuildExecutorOffline(job);
                            save(job);
                        });
                    } else {
                        logger.onError(query.cause());
                    }
                });
    }

    @Override
    public Future<Void> cancel(BuildJob job) {
        logs.add(job.getId(), "Requesting executor " + job.getInstance() + " to terminate process..");
        DeliveryOptions delivery = new DeliveryOptions();
        core.bus().publish(BUS_CANCEL, Serializer.json(job), delivery);
        return Future.succeededFuture();
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
    public Future<BuildJob> buildById(String buildId) {
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
    public Future<Collection<BuildJob>> history(String repository, String branch) {
        Future<Collection<BuildJob>> future = Future.future();
        QueryBuilder<BuildJob> query = builds.query(ID);

        if (isQuerying(repository)) {
            query.and(CONFIG_REPOSITORY).equalTo(repository);
        }
        if (isQuerying(branch)) {
            query.and(CONFIG_BRANCH).equalTo(branch);
        }

        query.pageSize(Configurations.storage().getMaxResults()).page(0)
                .orderBy(START)
                .order(SortOrder.DESCENDING)
                .execute(search -> {
                    if (search.succeeded()) {
                        future.complete(search.result());
                    } else {
                        future.fail(search.cause());
                    }
                });
        return future;
    }

    private boolean isQuerying(String input) {
        return input != null && !input.isEmpty() && !CoreStrings.ANY.equalsIgnoreCase(input);
    }

    @Override
    public Future<Collection<LogEvent>> logByIdWithOffset(String buildId, Long time) {
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
                query.result().forEach(job -> logs.clear(job.getId()).setHandler(logRemove -> {
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
                }));
            } else {
                future.fail(query.cause());
            }
        });
        return future;
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
}
