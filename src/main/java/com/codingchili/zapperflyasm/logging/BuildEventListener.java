package com.codingchili.zapperflyasm.logging;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;

/**
 * @author Robin Duda
 *
 * Listener interface for build status changes.
 */
public interface BuildEventListener {

    /**
     * @param job
     */
    default void onBuildQueued(BuildJob job) {
    }

    /**
     * @param job
     */
    default void onBuildStarted(BuildJob job) {
    }

    /**
     * @param job
     */
    default void onBuildComplete(BuildJob job) {
    }

    /**
     * @param job
     */
    default void onBuildExecutorOffline(BuildJob job) {
    }

    /**
     * @param info
     */
    default void onBuildExecutorOffline(InstanceInfo info) {
    }
}
