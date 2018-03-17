package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.User;

import java.util.HashMap;
import java.util.Map;

import com.codingchili.core.configuration.Configurable;
import com.codingchili.core.files.Configurations;

/**
 * @author Robin Duda
 * <p>
 * Contains configuration for the server.
 * <p>
 * Configuration needs only to exist on a single node :)
 * That node must be alive when other nodes are starting,
 * but can go offline without interrupting any services.
 */
public class ZapperConfig implements Configurable {
    public static String PATH = "config.yaml";
    private static ZapperConfig config = null;
    private Map<String, User> users = new HashMap<>();
    private Integer timeoutSeconds = 300;
    private String buildPath;

    /**
     * @return a list of configured users.
     */
    public Map<String, User> getUsers() {
        return users;
    }

    /**
     * @param users a list of users.
     */
    public void setUsers(Map<String, User> users) {
        this.users = users;
    }

    /**
     * @return the timeout in seconds per build.
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String getPath() {
        return PATH;
    }

    /**
     * @return the loaded configuration.
     */
    public static ZapperConfig get() {
        if (config == null) {
            config = Configurations.get(PATH, ZapperConfig.class);
        }
        return config;
    }

    /**
     * @return base path for where to store builds.
     */
    public String getBuildPath() {
        return buildPath;
    }

    public void setBuildPath(String buildPath) {
        this.buildPath = buildPath;
    }
}
