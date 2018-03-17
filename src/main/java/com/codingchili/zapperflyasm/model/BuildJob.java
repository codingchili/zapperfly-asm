package com.codingchili.zapperflyasm.model;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Robin Duda
 * <p>
 * Represents a single build that is executing or is to be executed.
 */
public class BuildJob {
    private ZonedDateTime start = ZonedDateTime.now();
    private ZonedDateTime end;
    private List<String> log = new ArrayList<>();
    private List<String> outputDirs = Arrays.asList("out", "build", "target");
    private String id = UUID.randomUUID().toString();
    private String message;
    private String instance;
    private String commit;
    private Status status = Status.QUEUED;
    private BuildConfiguration config;
    private String directory;

    public BuildJob(BuildConfiguration config) {
        this.config = config;
    }

    /**
     * @return the commandline that will be executed to start the build.
     */
    public String getCmdLine() {
        return config.getCmdLine();
    }

    /**
     * @return the branch on which the build is running.
     */
    public String getBranch() {
        return config.getBranch();
    }


    /**
     * @return the commit SHA1 hash that the build is executing on.
     */
    public String getCommit() {
        return commit;
    }

    /**
     * @return the repository the branch exists in.
     */
    public String getRepository() {
        return config.getRepository();
    }

    /**
     * @return a list of lines that is the output of the build execution.
     */
    public List<String> getLog() {
        return log;
    }

    public void setLog(List<String> log) {
        this.log = log;
    }

    /**
     * @return the build status.
     */
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;

        if (status.equals(Status.DONE) || status.equals(Status.FAILED) || status.equals(Status.CANCELLED)) {
            this.end = ZonedDateTime.now();
        }
    }

    /**
     * @return the id of this particular build.
     */
    public String getId() {
        return id;
    }

    /**
     * @return a message that can be used to display a failure.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the start date of the build.
     */
    public ZonedDateTime getStart() {
        return start;
    }

    /**
     * @return the end date of the build.
     */
    public ZonedDateTime getEnd() {
        return end;
    }

    /**
     * @return the command line that is executed to start the build.
     */
    public String getCmdline() {
        return config.getCmdLine();
    }

    /**
     * @return a list of output paths from where to locate artifacts for download.
     */
    public List<String> getOutputDirs() {
        return outputDirs;
    }

    /**
     * @return if true then no artifacts will be stored and the build will delete itself.
     */
    public boolean isAutoclean() {
        return config.isAutoclean();
    }

    /**
     * @return the directory where the build is executing.
     */
    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BuildJob) && ((BuildJob) obj).id.equals(id);
    }

    /**
     * @return creates a copy of this Build without the full log.
     */
    public BuildJob copyWithoutLog() {
        BuildJob clone = new BuildJob(config);
        clone.id = id;
        clone.message = message;
        clone.commit = commit;
        clone.status = status;
        clone.config = config;
        clone.instance = instance;
        clone.end = end;
        clone.start = start;
        clone.directory = directory;
        clone.outputDirs = outputDirs;
        return null;
    }
}
