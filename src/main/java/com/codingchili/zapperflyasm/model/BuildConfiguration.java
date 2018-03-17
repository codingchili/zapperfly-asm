package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 * <p>
 * Contains build configuration for a repo and branch combination.
 */
public class BuildConfiguration {
    private boolean autoclean = false;
    private String repository;
    private String branch;
    private String cmdLine;

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

    /**
     * @return the commandline to be executed.
     */
    public String getCmdLine() {
        return cmdLine;
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

    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }
}
