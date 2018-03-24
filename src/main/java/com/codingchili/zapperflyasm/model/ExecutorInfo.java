package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 * <p>
 * Contains information about an executor that is online.
 */
public class ExecutorInfo {
    private String instance;
    private boolean online;
    private int builds;
    private int capacity;

    /**
     * @return the name of the instance.
     */
    public String getInstance() {
        return instance;
    }

    public ExecutorInfo setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    /**
     * @return the number of builds in progress.
     */
    public int getBuilds() {
        return builds;
    }

    public ExecutorInfo setBuilds(int builds) {
        this.builds = builds;
        return this;
    }

    /**
     * @return the maximum number of concurrent builds.
     */
    public int getCapacity() {
        return capacity;
    }

    public ExecutorInfo setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * @return true if the executor is currently online - false if it is not online.
     * Executors that has been offline for a while is removed from the list of
     * available executors.
     */
    public boolean isOnline() {
        return online;
    }

    public ExecutorInfo setOnline(boolean online) {
        this.online = online;
        return this;
    }
}
