package com.codingchili.zapperflyasm.building;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;
import com.codingchili.zapperflyasm.model.BuildConfiguration;
import com.codingchili.zapperflyasm.logging.LogEvent;
import io.vertx.core.Future;

import java.util.Collection;
import java.util.List;

/**
 * @author Robin Duda
 * <p>
 * Interface for the build manager.
 */
public interface BuildManager {

    /**
     * Called when the context is loaded.
     * @param zapper the zapper context to be used for this manager.
     */
    void init(ZapperContext zapper);

    /**
     * Schedules a build for execution on the repo and branch in the given config.
     *
     * @param job a new job to execute, contains the configuration to be used.
     * @return the job that was scheduled.
     */
    Future<BuildJob> submit(BuildJob job);

    /**
     * Lists all avilable instances that have joined the cluster at some point.
     * This method may return instances that are offline.
     *
     * @return callback.
     */
    Future<List<InstanceInfo>> instances();

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
    Future<BuildJob> buildById(String buildId);

    /**
     * @return a list of builds that has been queued.
     */
    Future<Collection<BuildJob>> queued();

    /**
     * @return all builds that are either finished or in progress.
     * @param repository the repository to get builds from, may be * or null.
     * @param branch the branches to get builds from, may be * or null.
     */
    Future<Collection<BuildJob>> history(String repository, String branch);

    /**
     * Retrieves the log of the given build starting from the given line number.
     *
     * @param buildId the build to retrieve the logs from.
     * @param time the time of the last read log message.
     * @return callback.
     */
    Future<Collection<LogEvent>> logByIdWithOffset(String buildId, Long time);

    /**
     * Removes all build history.
     *
     * @return callback.
     */
    Future<Void> clear();
}
