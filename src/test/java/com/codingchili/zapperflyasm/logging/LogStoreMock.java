package com.codingchili.zapperflyasm.logging;

import com.codingchili.zapperflyasm.model.BuildJob;
import io.vertx.core.Future;

import java.util.*;

/**
 * @author Robin Duda
 *
 * Mock implementation of a log store.
 */
public class LogStoreMock implements LogStore {
    private Map<String, Collection<LogEvent>> logs = new HashMap<>();

    @Override
    public Future<Void> clear(String buildId) {
        logs.remove(buildId);
        return Future.succeededFuture();
    }

    @Override
    public void addListener(BuildEventListener listener) {

    }

    @Override
    public Future<Void> add(String buildId, String line) {
        Collection<LogEvent> logEvents = logs.computeIfAbsent(buildId, (key) -> new ArrayList<>());
        logEvents.add(new LogEvent(line));
        return Future.succeededFuture();
    }

    @Override
    public Future<Collection<LogEvent>> retrieve(String buildId, Long offsetMS) {
        ArrayList<LogEvent> events = new ArrayList<>();
        events.add(new LogEvent());
        return Future.succeededFuture(logs.getOrDefault(buildId, events));
    }

    @Override
    public void onBuildQueued(BuildJob job) {
        //
    }

    @Override
    public void onBuildStarted(BuildJob job) {
        //
    }

    @Override
    public void onBuildComplete(BuildJob job) {
        //
    }
}
