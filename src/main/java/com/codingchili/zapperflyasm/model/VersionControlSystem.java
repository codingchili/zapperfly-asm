package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;

import java.util.List;

/**
 * @author Robin Duda
 * <p>
 * An interface for a version control system.
 */
public interface VersionControlSystem {

    /**
     * Clones the branch from the configured repository.
     *
     * @param job contains the repo and
     * @return return the location the files was cloned to.
     */
    Future<String> clone(BuildJob job);

    /**
     * Lists the available artifacts for the given build.
     *
     * @param buildJob the job to list artifacts for.
     * @return a list of files that exists in the jobs output folders.
     */
    Future<List<String>> artifacts(BuildJob buildJob);

    /**
     * Removes files for the given job.
     *
     * @param job the job to delete files for.
     * @return callback: completed when files are deleted.
     */
    Future<Void> delete(BuildJob job);
}
