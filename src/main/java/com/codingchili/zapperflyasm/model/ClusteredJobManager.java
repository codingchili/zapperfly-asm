package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;
import com.codingchili.core.storage.*;

import static com.codingchili.zapperflyasm.model.BuildJob.START;
import static com.codingchili.zapperflyasm.model.BuildRequest.*;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - distributes across the cluster.
 */
public class ClusteredJobManager implements JobManager {
    public static final String BUILD = "build";
    public static final String PROGRESS = "progress";
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<BuildJob> jobs;
    private AsyncStorage<LogEvent> logs;
    private AsyncStorage<InstanceInfo> instances;
    private VersionControlSystem vcs;
    private InstanceInfo instance = new InstanceInfo();
    private BuildExecutor executor;
    private Logger logger;

    /**
     * Creates a new clustered job manager.
     *
     * @param core    the core on which the manager is to be run.
     * @param jobs    a potentially clustered store of jobs to execute.
     * @param logs    a potentially clulstered store to store job logs.
     * @param configs a potentially clustered store of build configuration.
     */
    public ClusteredJobManager(CoreContext core,
                               AsyncStorage<BuildJob> jobs,
                               AsyncStorage<LogEvent> logs,
                               AsyncStorage<BuildConfiguration> configs) {

        this.vcs = new GitExecutor(core);
        this.executor = new ProcessBuilderExecutor(core);
        this.jobs = jobs;
        this.configs = configs;
        this.logs = logs;
        this.logger = core.logger(getClass());

        ZapperConfig.getStorage(core, InstanceInfo.class).setHandler(done -> {
            if (done.succeeded()) {
                this.instances = done.result();
                save();
            } else {
                logger.onError(done.cause());
            }
        });

        core.periodic(() -> 1000, "pollQueue", (id) -> poll());
        core.periodic(() -> 5000, "saveInstance", (id) -> save());
    }

    private void save() {
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
                    .pageSize(1)
                    .page(0)
                    .execute(query -> {
                        if (query.succeeded() && query.result().size() > 0) {
                            begin();
                            BuildJob job = query.result().iterator().next();
                            job.setSaver(this::save);
                            job.setLogger(this::log);

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

    private void begin() {
        instance.setBuilds(instance.getBuilds() + 1);
        save();
    }

    private void complete() {
        instance.setBuilds(instance.getBuilds() - 1);
        save();
    }

    @Override
    public Future<BuildJob> submit(BuildConfiguration config) {
        Future<BuildJob> future = Future.future();
        BuildJob job = new BuildJob();
        job.setConfig(config);

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
        instances.values(future);
        return future;
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
    public Future<Void> putConfig(BuildConfiguration config) {
        Future<Void> future = Future.future();
        configs.put(config, future);
        return future;
    }

    @Override
    public Future<Void> removeConfig(String repository, String branch) {
        Future<Void> future = Future.future();
        configs.remove(BuildConfiguration.toKey(repository, branch), future);
        return future;
    }

    @Override
    public Future<BuildConfiguration> getConfig(String repository, String branch) {
        Future<BuildConfiguration> future = Future.future();
        configs.get(BuildConfiguration.toKey(repository, branch), future);
        return future;
    }

    @Override
    public Future<BuildJob> get(String buildId) {
        Future<BuildJob> future = Future.future();
        jobs.get(buildId, future);
        return future;
    }

    @Override
    public Future<Collection<BuildConfiguration>> getAllConfigs() {
        Future<Collection<BuildConfiguration>> future = Future.future();
        configs.values(future);
        return future;
    }

    @Override
    public Future<Collection<BuildJob>> getAll() {
        Future<Collection<BuildJob>> future = Future.future();
        jobs.query(ID_BUILD).matches(".*")
                .pageSize(24).page(0)
                .orderBy(START)
                .order(SortOrder.ASCENDING)
                .execute(future);
        return future;
    }

    @Override
    public Future<Collection<LogEvent>> getLog(String buildId, int logOffset) {
        Future<Collection<LogEvent>> future = Future.future();
        QueryBuilder<LogEvent> query = logs.query(BUILD).equalTo(buildId)
                .orderBy(ID_TIME)
                .order(SortOrder.ASCENDING);

        if (logOffset > 0) {
            query.pageSize(logOffset).page(1);
        }

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

    public void setVCSProvider(VersionControlSystem vcs) {
        this.vcs = vcs;
    }

    public void setBuildExecutor(BuildExecutor executor) {
        this.executor = executor;
    }
}