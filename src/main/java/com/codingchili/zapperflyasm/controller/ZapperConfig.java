package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.User;
import io.vertx.core.Future;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.codingchili.core.configuration.Configurable;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.files.Configurations;
import com.codingchili.core.storage.*;

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
    private String storage = HazelMap.class.getName();
    private Integer timeoutSeconds = 300;
    private String buildPath = Paths.get("").toAbsolutePath().toString();
    private int capacity = 1;


    public static <T extends Storable> Future<AsyncStorage<T>> getStorage(CoreContext core,
                                                                          Class<T> value) {
        Future<AsyncStorage<T>> future = Future.future();
        new StorageLoader<T>(core)
                .withPlugin(get().getStorage())
                .withValue(value)
                .withDB(value.getSimpleName())
                .build(done -> {
                    if (done.succeeded()) {
                        future.complete(done.result());
                    } else {
                        future.fail(done.cause());
                    }
                });
        return future;
    }

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

    /**
     * @param buildPath the path where builds are cloned into and executed.
     */
    public void setBuildPath(String buildPath) {
        this.buildPath = buildPath;
    }

    /**
     * @return a class that implements AsyncStorage.
     */
    public String getStorage() {
        return storage;
    }

    /**
     * @param storage the implementation to use for job and build configuration.
     */
    public void setStorage(String storage) {
        this.storage = storage;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
