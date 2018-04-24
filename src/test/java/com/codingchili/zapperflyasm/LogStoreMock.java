package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.model.LogEvent;
import com.codingchili.zapperflyasm.model.LogStore;
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
    public Future<Void> add(String buildId, LogEvent event) {
        Collection<LogEvent> logEvents = logs.computeIfAbsent(buildId, (key) -> new ArrayList<>());
        logEvents.add(event);
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> clear(String buildId) {
        logs.remove(buildId);
        return Future.succeededFuture();
    }

    @Override
    public Future<Collection<LogEvent>> retrieve(String buildId, Long offsetMS) {
        ArrayList<LogEvent> events = new ArrayList<>();
        events.add(new LogEvent());
        return Future.succeededFuture(logs.getOrDefault(buildId, events));
    }
}
