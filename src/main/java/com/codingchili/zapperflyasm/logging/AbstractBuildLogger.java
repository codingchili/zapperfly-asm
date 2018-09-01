package com.codingchili.zapperflyasm.logging;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Robin Duda
 *
 * Abstract class to handle multiple listeners for events.
 */
public abstract class AbstractBuildLogger implements BuildEventListener, LogStore {
    private Collection<BuildEventListener> listeners = new ArrayList<>();

    @Override
    public void addListener(BuildEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onBuildStarted(BuildJob job) {
        listeners.forEach(listener -> listener.onBuildStarted(job));
    }

    @Override
    public void onBuildComplete(BuildJob job) {
        listeners.forEach(listener -> listener.onBuildComplete(job));
    }

    @Override
    public void onBuildQueued(BuildJob job) {
        listeners.forEach(listener -> listener.onBuildQueued(job));
    }

    @Override
    public void onBuildExecutorOffline(BuildJob job) {
        listeners.forEach(listener -> listener.onBuildExecutorOffline(job));
    }

    @Override
    public void onBuildExecutorOffline(InstanceInfo info) {
        listeners.forEach(listener -> listener.onBuildExecutorOffline(info));
    }
}
