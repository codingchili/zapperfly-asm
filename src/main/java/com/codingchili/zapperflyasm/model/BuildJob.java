package com.codingchili.zapperflyasm.model;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Represents a single build that is executing or is to be executed.
 */
public class BuildJob implements Storable {
    public static final String START = "start";
    private transient Consumer<BuildJob> saver;
    private transient BiConsumer<BuildJob, String> logger;
    private BuildConfiguration config;
    private Long start = System.currentTimeMillis();
    private Long end = 0L;
    private String id = UUID.randomUUID().toString();
    private String instance;
    private String message;
    private String commit;
    private Status progress = Status.QUEUED;
    private String directory;
    private String author;
    private String fullCommit;

    public BuildJob() {
    }

    public BuildJob(BuildConfiguration config, Consumer<BuildJob> saver, BiConsumer<BuildJob, String> logger) {
        this.config = config;
        this.saver = saver;
        this.logger = logger;
    }

    public void log(String line) {
        if (logger != null) {
            logger.accept(this, line);
        }
    }

    @Override
    protected Object clone() {
        throw new CoreRuntimeException("not really supported.");
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

    public BuildJob setConfig(BuildConfiguration config) {
        this.config = config;
        return this;
    }

    /**
     * @return the build status.
     */
    public Status getProgress() {
        return progress;
    }

    public void setProgress(Status progress) {
        if (!this.progress.isFinal()) {
            this.progress = progress;

            if (progress.isFinal()) {
                this.end = ZonedDateTime.now().toInstant().toEpochMilli();
            }

            if (saver != null) {
                saver.accept(this);
            }
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
        if (directory == null) {
            String path = ZapperConfig.getEnvironment().getBuildPath();
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            directory = path + id;
        }
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the author of the commit.
     */
    public String getAuthor() {
        return author;
    }

    public void setFullCommit(String fullCommit) {
        this.fullCommit = fullCommit;
    }

    /**
     * @return the full commit hash that triggered the build.
     */
    public String getFullCommit() {
        return fullCommit;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BuildJob) && ((BuildJob) obj).id.equals(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] on branch %s in repo %s.", id, config.getBranch(), config.getRepository());
    }

    @Override
    public int compareToAttribute(Storable other, String attribute) {
        if (attribute.equals(START)) {
            return start.compareTo(((BuildJob) other).start);
        } else {
            return 0;
        }
    }

    /**
     * @param logger the logger to use for this instance.
     */
    public void setLogger(BiConsumer<BuildJob, String> logger) {
        this.logger = logger;
    }

    /**
     * @param saver the saver to use for this instance.
     */
    public void setSaver(Consumer<BuildJob> saver) {
        this.saver = saver;
    }

    /**
     * Attempts to commit changes to the job to storage.
     */
    public void save() {
        if (saver != null) {
            saver.accept(this);
        }
    }
}
