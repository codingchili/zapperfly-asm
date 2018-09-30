package com.codingchili.zapperflyasm.model;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Contains information about an executor that is online.
 */
public class InstanceInfo implements Storable {
    private static InstanceInfo info = new InstanceInfo();
    private String instance;
    private String webserver;
    private boolean online = true;
    private double cpu = 0.0;
    private double mem = 0.0;
    private int builds = 0;
    private int webserverPort;
    private int capacity;
    private long updated;

    /**
     * @return the instance information.
     */
    public static InstanceInfo get() {
        return info;
    }

    public InstanceInfo() {
        ZapperConfig config = ZapperConfig.get();
        instance = config.getEnvironment().getInstanceName();
        capacity = config.getEnvironment().getCapacity();
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
     * @return URL to the webserver - unset if not running webserver.
     */
    public String getWebserver() {
        return webserver;
    }

    public void setWebserver(String webserver) {
        this.webserver = webserver;
    }

    /**
     * @return the port that the webserver is running on, 0 if not.
     */
    public int getWebserverPort() {
        return webserverPort;
    }

    public void setWebserverPort(int webserverPort) {
        this.webserverPort = webserverPort;
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

    /**
     * Called if the instance is started with website enabled.
     */
    public void setWebsiteEnabled(int port) {
        webserver = Environment.hostname().orElse("localhost");
        webserverPort = port;
    }
}
