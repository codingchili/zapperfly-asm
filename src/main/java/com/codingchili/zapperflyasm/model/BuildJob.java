package com.codingchili.zapperflyasm.model;

import java.time.ZonedDateTime;
import java.util.*;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Represents a single build that is executing or is to be executed.
 */
public class BuildJob implements Storable {
    private Collection<LogEvent> log = new ArrayList<>();
    private BuildConfiguration config;
    private Long start = ZonedDateTime.now().toEpochSecond();
    private Long end;
    private String id = UUID.randomUUID().toString();
    private String instance = Environment.hostname().orElseGet(() -> UUID.randomUUID().toString());
    private String message;
    private String commit;
    private Status progress = Status.QUEUED;
    private String directory;

    public BuildJob() {}

    public BuildJob(BuildConfiguration config) {
        this.config = config;
    }

    /**
     * @return the commit SHA1 hash that the build is executing on.
     */
    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    /**
     * @return a list of lines that is the output of the build execution.
     */
    public Collection<LogEvent> getLog() {
        return log;
    }

    public void setLog(Collection<LogEvent> log) {
        this.log = log;
    }

    /**
     * @param line a line to log.
     */
    public void log(String line) {
        LogEvent event = new LogEvent();
        event.setTime(ZonedDateTime.now().toEpochSecond());
        event.setLine(line);
        log.add(event);
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the id of the instance of where the build is executing.
     */
    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * @return configuration specific to this branch and repository.
     */
    public BuildConfiguration getConfig() {
        return config;
    }

    public void setConfig(BuildConfiguration config) {
        this.config = config;
    }

    /**
     * @return the build status.
     */
    public Status getProgress() {
        return progress;
    }

    public void setProgress(Status progress) {
        this.progress = progress;

        if (progress.equals(Status.DONE) || progress.equals(Status.FAILED) || progress.equals(Status.CANCELLED)) {
            this.end = ZonedDateTime.now().toEpochSecond();
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
    public Long getStart() {
        return start;
    }

    /**
     * @return the end date of the build.
     */
    public Long getEnd() {
        return end;
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
        clone.progress = progress;
        clone.config = config;
        clone.instance = instance;
        clone.end = end;
        clone.start = start;
        clone.directory = directory;
        return clone;
    }

    @Override
    public String toString() {
        return String.format("[%s] on branch %s in repo %s.", id, config.getBranch(), config.getRepository());
    }
}
