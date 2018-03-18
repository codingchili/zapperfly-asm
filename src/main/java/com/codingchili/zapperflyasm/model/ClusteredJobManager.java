package com.codingchili.zapperflyasm.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.Collection;
import java.util.List;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.storage.AsyncStorage;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - distributes across the cluster.
 */
public class ClusteredJobManager implements JobManager {
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<BuildJob> jobs;
    private VersionControlSystem vcs;
    private BuildExecutor executor;

    /**
     * Creates a new clustered job manager.
     *
     * @param core    the core on which the manager is to be run.
     * @param jobs    a potentially clustered store of jobs to execute.
     * @param configs a potentially clustered store of build configuration.
     */
    public ClusteredJobManager(CoreContext core,
                               AsyncStorage<BuildJob> jobs,
                               AsyncStorage<BuildConfiguration> configs) {

        this.vcs = new GitExecutor(core);
        this.executor = new ProcessBuilderExecutor(core);
        this.jobs = jobs;
        this.configs = configs;
    }

    @Override
    public Future<BuildJob> submit(BuildConfiguration config) {
        Future<BuildJob> future = Future.future();
        BuildJob job = new BuildJob(config);

        // add the job to the queue.
        jobs.put(job, (put) -> {

            if (put.succeeded()) {
                // clone the repository and the branch.
                vcs.clone(job).setHandler(clone -> {
                    if (clone.succeeded()) {
                        // cloned ok - start building.
                        executor.build(job).setHandler(
                                (executed) -> handleCompleted(executed, job));
                    } else {
                        throw new CoreRuntimeException(clone.cause().getMessage());
                    }
                });
            } else {
                future.fail(put.cause());
            }
        });
        return future;
    }

    private void handleCompleted(AsyncResult<Void> build, BuildJob job) {
        if (build.succeeded() && job.getConfig().isAutoclean()) {
            vcs.delete(job);
        }
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
    public Future<Collection<BuildJob>> getAll() {
        Future<Collection<BuildJob>> future = Future.future();
        jobs.values(future);
        return future;
    }

    public void setVCSProvider(VersionControlSystem vcs) {
        this.vcs = vcs;
    }

    public void setBuildExecutor(BuildExecutor executor) {
        this.executor = executor;
    }
}
