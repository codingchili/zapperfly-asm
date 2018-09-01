package com.codingchili.zapperflyasm.logging;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Future;

import java.util.Collection;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * A hazelcast implementation of a log store.
 * This could be mongodb, elasticsearch or any other database really.
 */
public class HazelLogStore extends AbstractBuildLogger implements LogStore {
    private HazelcastInstance instance;
    private CoreContext context;
    private Logger logger;

    private HazelLogStore(CoreContext context) {
        this.context = context;
        this.logger = context.logger(getClass());
        addListener(this);
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
    public Future<Void> add(String buildId, String line) {
        if (line != null && !line.isEmpty()) {
            Future<Void> future = Future.future();
            context.blocking((blocking) -> {
                try {
                    instance.getList(buildId).add(new LogEvent(line));
                } catch (Throwable e) {
                    logger.onError(e);
                }
                blocking.complete();
            }, future);
            return future;
        }
        return Future.succeededFuture();
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

    @Override
    public void onBuildStarted(BuildJob job) {
        add(job.getId(), "Build starting on executor '" + InstanceInfo.get().getId() + "' ..");
    }

    @Override
    public void onBuildQueued(BuildJob job) {
        add(job.getId(), "Build " + job.getId() + " queued.");
    }

    @Override
    public void onBuildExecutorOffline(BuildJob job) {
        add(job.getId(), String.format("'%s' has gone offline - build failed.", job.getInstance()));
    }
}
