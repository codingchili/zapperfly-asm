package com.codingchili.zapperflyasm.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.codingchili.core.context.CoreContext;

/**
 * @author Robin Duda
 */
public class SimpleJobManager implements JobManager {
    // todo: implement queueing.
    // todo: implement clustering.
    // todo: configuration for parallelism.
    private Queue<BuildJob> queue = new ConcurrentLinkedQueue<>();
    private Map<String, BuildConfiguration> configs = new HashMap<>();
    private Map<String, BuildJob> jobs = new HashMap<>();
    private GitExecutor git;
    private BuildExecutor executor;

    public SimpleJobManager(CoreContext core) {
        git = new GitExecutor(core);
        executor = new BuildExecutor(core);
    }

    @Override
    public BuildJob submit(BuildConfiguration config) {
        BuildJob job = new BuildJob(config);

        // add the job to the queue.
        jobs.put(job.getId(), job);
        queue.add(job);

        // clone the repository and the branch.
        git.clone(job).setHandler(clone -> {
            if (clone.succeeded()) {
                // cloned ok - start building.
                executor.build(job).setHandler(
                        (done) -> handleCompleted(done, job));
            }
        });
        return job;
    }

    private void handleCompleted(AsyncResult<Void> build, BuildJob job) {
        if (build.succeeded() && job.isAutoclean()) {
            git.delete(job);
        }
    }

    @Override
    public Future<Void> delete(BuildJob job) {
        Future<Void> future = Future.future();
        git.delete(job).setHandler(future);
        return future;
    }

    @Override
    public Future<List<String>> artifacts(BuildJob job) {
        return git.artifacts(job);
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
}
