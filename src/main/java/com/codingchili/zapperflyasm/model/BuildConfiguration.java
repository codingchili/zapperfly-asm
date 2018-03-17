package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 */
public class BuildConfiguration {
    private boolean autoclean = false;
    private String repository;
    private String branch;
    private String cmdLine;

    public String getRepository() {
        return repository;
    }

    public String getBranch() {
        return branch;
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    public String getCmdLine() {
        return cmdLine;
    }

    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }

    public String getId() {
        return toKey(repository, branch);
    }

    public static String toKey(String repository, String branch) {
        return repository + "@" + branch;
    }
}
