package com.codingchili.zapperflyasm.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 * <p>
 * Manages jobs - does not support clustering yet.
 */
public class SimpleJobManager implements JobManager {
    private Queue<BuildJob> queue = new ConcurrentLinkedQueue<>();
    private Map<String, BuildConfiguration> configs = new HashMap<>();
    private Map<String, BuildJob> jobs = new HashMap<>();
    private VersionControlSystem vcs;
    private ProcessBuilderExecutor executor;

    public SimpleJobManager(CoreContext core) {
        vcs = new GitExecutor(core);
        executor = new ProcessBuilderExecutor(core);
    }

    @Override
    public BuildJob submit(BuildConfiguration config) {
        BuildJob job = new BuildJob(config);

        // add the job to the queue.
        jobs.put(job.getId(), job);
        queue.add(job);

        // clone the repository and the branch.
        vcs.clone(job).setHandler(clone -> {
            if (clone.succeeded()) {
                // cloned ok - start building.
                executor.build(job).setHandler(
                        (done) -> handleCompleted(done, job));
            } else {
                throw new CoreRuntimeException(clone.cause().getMessage());
            }
        });
        return job;
    }

    private void handleCompleted(AsyncResult<Void> build, BuildJob job) {
        if (build.succeeded() && job.isAutoclean()) {
            vcs.delete(job);
        }
    }

    @Override
    public void cancel(BuildJob job) {
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
    public void putConfig(BuildConfiguration config) {
        configs.put(config.getId(), config);
    }

    @Override
    public void removeConfig(String repository, String branch) {
        configs.remove(BuildConfiguration.toKey(repository, branch));
    }

    @Override
    public BuildConfiguration getConfig(String repository, String branch) {
        return configs.get(BuildConfiguration.toKey(repository, branch));
    }

    @Override
    public BuildJob get(String buildId) {
        BuildJob job = jobs.get(buildId);

        if (job == null) {
            throw new NoSuchBuildException(buildId);
        }
        return job;
    }

    @Override
    public Collection<BuildJob> getAll() {
        return jobs.values();
    }

    public void setVCSProvider(VersionControlSystem vcs) {
        this.vcs = vcs;
    }
}
