package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;

import java.util.Collection;

/**
 * @author Robin Duda
 * <p>
 * An interface for an implementation that stores log events from a build.
 */
public interface LogStore {

    /**
     * Adds a new log entry to the log store.
     *
     * @param buildId the ID of the build that generated the event.
     * @param event   the log event to be stored.
     * @return future completed when the log event is added.
     */
    Future<Void> add(String buildId, LogEvent event);

    /**
     * Retrieves all log events that has occured after the given time.
     *
     * @param buildId the ID of the build to retrieve log events from.
     * @return future completed when the log events are retrieved.
     */
    Future<Collection<LogEvent>> retrieve(String buildId, Long epochSecondsOffset);

    /**
     * Removes all log entries associated with the given build.
     *
     * @param buildId the ID of the build to remove log events for.
     * @return future completed when log items have been removed.
     */
    Future<Void> clear(String buildId);
}
