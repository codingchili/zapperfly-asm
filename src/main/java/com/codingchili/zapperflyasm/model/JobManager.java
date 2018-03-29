package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;

import java.util.Collection;
import java.util.List;

/**
 * @author Robin Duda
 * <p>
 * Interface for the build manager.
 */
public interface JobManager {

    /**
     * Schedules a build for execution on the repo and branch in the given config.
     *
     * @param config specifies which repo and on which branch to execute the configured
     *               command line. Also includes the artifact output folders.
     * @return the job that was scheduled.
     */
    Future<BuildJob> submit(BuildConfiguration config);

    /**
     * Lists all avilable instances that have joined the cluster at some point.
     * This method may return instances that are offline.
     *
     * @return callback.
     */
    Future<Collection<InstanceInfo>> instances();

    /**
     * Cancels a build that is in progress.
     *
     * @param buildJob the build job to cancel.
     */
    Future<Void> cancel(BuildJob buildJob);

    /**
     * Removes a build from the history and its files on disk.
     *
     * @param job the job to remove.
     * @return callback: completed on deletion.
     */
    Future<Void> delete(BuildJob job);

    /**
     * Lists available artifacts for the given job.
     *
     * @param job the job to list artifacts for.
     * @return callback: a list of files available for download.
     */
    Future<List<String>> artifacts(BuildJob job);

    /**
     * Returns build information for the given build ID.
     *
     * @param buildId the ID of the build to retrieve.
     * @return a build matching the given build ID.
     */
    Future<BuildJob> getBuild(String buildId);

    /**
     * Adds configuration for a repo and branch combination.
     *
     * @param config the configuration to add.
     */
    Future<Void> putConfig(BuildConfiguration config);

    /**
     * Removes configuration for the given repo and branch.
     *
     * @param repository the repo to remove configuration for.
     * @param branch     the branch to remove configuration for.
     */
    Future<Void> removeConfig(String repository, String branch);

    /**
     * Retrieves configuration for the given repo and branch.
     *
     * @param repository the repository to retrieve config of.
     * @param branch     the branch to retrieve config of.
     * @return build configuration for the given combination of repo and branch.
     */
    Future<BuildConfiguration> getConfig(String repository, String branch);

    /**
     * Lists all available configurations.
     *
     * @return all configurations that exists.
     */
    Future<Collection<BuildConfiguration>> getAllConfigs();

    /**
     * @return all builds that has ever been scheduled.
     */
    Future<Collection<BuildJob>> getAll();

    /**
     * Retrieves the log of the given build starting from the given line number.
     *
     * @param buildId the build to retrieve the logs from.
     * @param time the time of the last read log message.
     * @return callback.
     */
    Future<Collection<LogEvent>> getLog(String buildId, Long time);
}
