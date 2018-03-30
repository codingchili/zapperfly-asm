package com.codingchili.zapperflyasm.model;

import java.util.Arrays;
import java.util.List;

import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Contains build configuration for a repo and branch combination.
 */
public class BuildConfiguration implements Storable {
    private List<String> outputDirs = Arrays.asList("out", "build", "target");
    private boolean autoclean = false;
    private String repository;
    private String branch;
    private String cmdLine;

    public BuildConfiguration() {}

    /**
     * @param repository the repository the configuration applies to.
     * @param branch the branch the configuration applies to.
     */
    public BuildConfiguration(String repository, String branch) {
        this.repository = repository;
        this.branch = branch;
    }

    /**
     * @return the repository for which the configuration applies.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @return the branch for which the configuration applies.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @return true if the build catalog is to be removed when builds complete.
     */
    public boolean isAutoclean() {
        return autoclean;
    }

    public void setOutputDirs(List<String> outputDirs) {
        this.outputDirs = outputDirs;
    }

    /**
     * @return a list of output paths from where to locate artifacts for download.
     */
    public List<String> getOutputDirs() {
        return outputDirs;
    }

    /**
     * @return the commandline to be executed.
     */
    public String getCmdLine() {
        return cmdLine;
    }

    /**
     * @param cmdLine the commandline to execute when starting the build.
     */
    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * @return the unique id of the configuration.
     */
    public String getId() {
        return toKey(repository, branch);
    }

    /**
     * Converts the given repo and branch into a unique identifier for the config.
     *
     * @param repository the name of the repository.
     * @param branch     the name of the branch.
     * @return a unique key.
     */
    public static String toKey(String repository, String branch) {
        return repository + "@" + branch;
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
