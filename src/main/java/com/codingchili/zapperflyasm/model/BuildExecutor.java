package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;

/**
 * @author Robin Duda
 *
 * Interface for the system that executes the actual build.
 */
public interface BuildExecutor {

    /**
     * Executes the given build job.
     *
     * @param job the build job to be executed. The configured commandline is
     *            passed to the processbuilder.
     * @return a future that completes when the build succeeds or fails.
     */
    Future<Void> build(BuildJob job);
}
