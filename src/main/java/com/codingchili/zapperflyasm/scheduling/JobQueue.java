package com.codingchili.zapperflyasm.scheduling;

import com.codingchili.zapperflyasm.model.BuildJob;
import io.vertx.core.Future;

import java.util.Collection;

/**
 * @author Robin Duda
 *
 * Interface for an implementation that queues jobs for the build manager.
 */
public interface JobQueue {

    /**
     * Adds a new build to the queue.
     * @param job the job that is to be queued.
     * @return a future completed when the job has been submitted to the queue.
     */
    Future<Void> submit(BuildJob job);

    /**
     * Returns all currently queued jobs.
     * @return callback.
     */
    Future<Collection<BuildJob>> values();

    /**
     * @param timeoutMS milliseconds to poll before completing.
     * @return callback: completed with result if there is an item in the queue
     * during the maximum waiting period. completed without result if there is
     * no items present during the window. failed if the queue cannot be polled.
     */
    Future<BuildJob> poll(int timeoutMS);
}
