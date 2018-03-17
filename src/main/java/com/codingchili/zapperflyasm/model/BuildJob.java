package com.codingchili.zapperflyasm.model;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Robin Duda
 */
public class BuildJob {
    private ZonedDateTime start = ZonedDateTime.now();
    private List<String> log = new ArrayList<>();
    private List<String> outputDirs = Arrays.asList("out", "build", "target");
    private String id = UUID.randomUUID().toString();
    private String message;
    private String instance;
    private Status status = Status.QUEUED;
    private BuildConfiguration config;

    public BuildJob(BuildConfiguration config) {
        this.config = config;
    }

    public String getCmdLine() {
        return config.getCmdLine();
    }

    public String getBranch() {
        return config.getBranch();
    }

    public String getRepository() {
        return config.getRepository();
    }

    public List<String> getLog() {
        return log;
    }

    public void setLog(List<String> log) {
        this.log = log;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public boolean isAutoclean() {
        return config.isAutoclean();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BuildJob) && ((BuildJob) obj).id.equals(id);
    }

    public String getCmdline() {
        return config.getCmdLine();
    }

    public List<String> getOutputDirs() {
        return outputDirs;
    }

    public void setOutputDirs(List<String> outputDirs) {
        this.outputDirs = outputDirs;
    }
}
