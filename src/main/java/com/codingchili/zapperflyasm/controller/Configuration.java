package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.User;

import java.util.HashMap;
import java.util.Map;

import com.codingchili.core.configuration.Configurable;

/**
 * @author Robin Duda
 * <p>
 * Contains configuration for the server.
 * <p>
 * Configuration needs only to exist on a single node :)
 * That node must be alive when other nodes are starting,
 * but can go offline without interrupting any services.
 */
public class Configuration implements Configurable {
    public static String PATH = "config.yaml";
    private Map<String, User> users = new HashMap<>();

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

    @Override
    public String getPath() {
        return PATH;
    }
}
