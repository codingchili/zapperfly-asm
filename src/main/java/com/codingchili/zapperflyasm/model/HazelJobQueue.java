package com.codingchili.zapperflyasm.model;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 */
public class HazelJobQueue implements JobQueue {
    private static final String QUEUE_NAME = "zapperfly.build.queue";
    public static final int POLL_WAIT = 2000;
    private CoreContext context;
    private HazelcastInstance instance;

    /**
     * @param context
     */
    private HazelJobQueue(CoreContext context) {
        this.context = context;
    }

    /**
     * @param context
     * @return future
     */
    public static Future<JobQueue> create(CoreContext context) {
        Future<JobQueue> future = Future.future();
        context.blocking((blocking) -> {
            HazelJobQueue queue = new HazelJobQueue(context);
            queue.instance = Hazelcast.getAllHazelcastInstances()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new CoreRuntimeException("Failed to get hazelcast instance."));
            blocking.complete(queue);
        }, future);
        return future;
    }

    @Override
    public Future<Void> submit(BuildJob job) {
        Future<Void> future = Future.future();
        context.blocking((blocking) -> {
            instance.getQueue(QUEUE_NAME).add(job);
            blocking.complete();
        }, future);
        return future;
    }

    @Override
    public Future<Collection<BuildJob>> values() {
        Future<Collection<BuildJob>> future = Future.future();
        context.blocking((blocking) -> {
            blocking.complete(new ArrayList<>(instance.getQueue(QUEUE_NAME)));
        }, future);
        return future;
    }

    @Override
    public Future<BuildJob> poll() {
        Future<BuildJob> future = Future.future();
        context.blocking((blocking) -> {
            try {
                BuildJob job = instance.<BuildJob>getQueue(QUEUE_NAME).poll(POLL_WAIT, TimeUnit.MILLISECONDS);
                if (job == null) {
                    blocking.complete();
                } else {
                    blocking.complete(job);
                }
            } catch (InterruptedException e) {
                blocking.fail(e);
            }
        }, future);
        return future;
    }
}
