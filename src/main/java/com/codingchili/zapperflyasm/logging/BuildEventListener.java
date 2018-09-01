package com.codingchili.zapperflyasm.logging;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;

/**
 * @author Robin Duda
 */
public interface BuildEventListener {

    /**
     * @param job
     */
    void onBuildQueued(BuildJob job);

    /**
     * @param job
     */
    void onBuildStarted(BuildJob job);

    /**
     * @param job
     */
    void onBuildComplete(BuildJob job);

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
