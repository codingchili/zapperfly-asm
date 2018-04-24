package com.codingchili.zapperflyasm.model;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Future;

import java.util.Collection;
import java.util.stream.Collectors;

import com.codingchili.core.context.*;

/**
 * @author Robin Duda
 * <p>
 * A hazelcast implementation of a log store.
 * This could be mongodb, elasticsearch or any other database really.
 */
public class HazelLogStore implements LogStore {
    private CoreContext context;
    private HazelcastInstance instance;

    private HazelLogStore(CoreContext context) {
        this.context = context;
    }

    /**
     * Asynchronous constructor with future.
     *
     * @param context the core context to create the log store on.
     * @return future completed when the log store is loaded.
     */
    public static Future<LogStore> create(CoreContext context) {
        Future<LogStore> future = Future.future();
        context.blocking((blocking) -> {
            HazelLogStore logs = new HazelLogStore(context);
            logs.instance = Hazelcast.getAllHazelcastInstances()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new CoreRuntimeException("Failed to get hazelcast instance."));
            blocking.complete(logs);
        }, future);
        return future;
    }

    @Override
    public Future<Void> add(String buildId, LogEvent event) {
        Future<Void> future = Future.future();
        context.blocking((blocking) -> {
            instance.getList(buildId).add(event);
            blocking.complete();
        }, future);
        return future;
    }

    @Override
    public Future<Collection<LogEvent>> retrieve(String buildId, Long epochOffsetSeconds) {
        Future<Collection<LogEvent>> future = Future.future();
        context.blocking((blocking) -> {
            blocking.complete(
                    instance.<LogEvent>getList(buildId)
                            .stream()
                            .filter(event -> event.getTime() > epochOffsetSeconds + 1)
                            .collect(Collectors.toList())
            );
        }, future);
        return future;
    }

    @Override
    public Future<Void> clear(String buildId) {
        Future<Void> future = Future.future();
        context.blocking((blocking) -> {
            instance.getList(buildId).clear();
            blocking.complete();
        }, future);
        return future;
    }
}
