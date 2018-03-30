package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Contains information about an executor that is online.
 */
public class InstanceInfo implements Storable {
    private String instance;
    private boolean online = true;
    private double cpu = 0.0;
    private double mem = 0.0;
    private int builds = 0;
    private int capacity;
    private long updated;

    public InstanceInfo() {
        ZapperConfig config = ZapperConfig.get();
        instance = config.getInstanceName();
        capacity = config.getCapacity();
    }

    public InstanceInfo setId(String id) {
        this.instance = id;
        return this;
    }

    /**
     * @return the number of builds in progress.
     */
    public int getBuilds() {
        return builds;
    }

    public InstanceInfo setBuilds(int builds) {
        this.builds = builds;
        return this;
    }

    /**
     * @return the maximum number of concurrent builds.
     */
    public int getCapacity() {
        return capacity;
    }

    public InstanceInfo setCapacity(int capacity) {
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

    public InstanceInfo setOnline(boolean online) {
        this.online = online;
        return this;
    }

    /**
     * @return last epoch MS when the instance info was saved.
     */
    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    /**
     * @return the cpu load in percent.
     */
    public double getCpu() {
        return cpu;
    }

    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    /**
     * @return memory utilization in percent.
     */
    public double getMem() {
        return mem;
    }

    public void setMem(double mem) {
        this.mem = mem;
    }

    @Override
    public String getId() {
        return instance;
    }

    public void calculateSystemLoad() {
        cpu = getMXBean().getSystemCpuLoad();
        mem = 1.0 - ((getMXBean().getFreePhysicalMemorySize() * 1.0) / getMXBean().getTotalPhysicalMemorySize());
    }

    private static OperatingSystemMXBean mxBean = null;
    private OperatingSystemMXBean  getMXBean() {
        if (mxBean == null) {
            mxBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                    .getOperatingSystemMXBean();
        }
        return mxBean;
    }
}
