package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.scheduling.JobQueue;
import io.vertx.core.Future;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Robin Duda
 *
 * Mock implementation of a job queue.
 */
public class JobQueueMock implements JobQueue {
    private Queue<BuildJob> queue = new LinkedBlockingQueue<>();

    @Override
    public Future<Void> submit(BuildJob job) {
        queue.add(job);
        return Future.succeededFuture();
    }

    @Override
    public Future<Collection<BuildJob>> values() {
        return Future.succeededFuture(new ArrayList<>(queue));
    }

    @Override
    public Future<BuildJob> poll() {
        Future<BuildJob> future = Future.future();
        future.complete(queue.poll());
        return future;
    }
}
