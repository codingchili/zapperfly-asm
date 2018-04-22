package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.User;
import io.vertx.core.Future;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.codingchili.core.configuration.Configurable;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.files.Configurations;
import com.codingchili.core.protocol.Serializer;
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
    private static String PATH = "zapperfly.yaml";
    private Map<String, User> users = new HashMap<>();
    private String storage = HazelMap.class.getName();
    private Integer timeoutSeconds = 300;
    private String buildPath = Paths.get("").toAbsolutePath().toString();
    private String dockerLine = "docker run -w /tmp/build -it -v '$directory:/tmp/build' --rm $image $script";
    private String groupName = "zapperfly-builds";
    private String windowsShell = "powershell.exe -Command";
    private String unixShell = "/bin/bash -E";
    private String instanceName = null;
    private int capacity = 2;

    /**
     * Retrieves a storage implementation used to host objects of the given class.
     *
     * @param core  the core context to create the storage on.
     * @param value the class of the value to be stored.
     * @param <T>   the type of the value to be stored.
     * @return callback.
     */
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
        return Configurations.get(PATH, ZapperConfig.class);
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
     * @return the template string to use when starting docker builds.
     */
    public String getDockerLine() {
        return dockerLine;
    }

    /**
     * @param dockerLine set the template string to use when starting docker builds.
     */
    public void setDockerLine(String dockerLine) {
        this.dockerLine = dockerLine;
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

    /**
     * @return name of the cluster group that shares jobs.
     */
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * @return the name of the instance.
     */
    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public static void main(String[] args) {
        System.out.println(Serializer.yaml(new ZapperConfig()));
    }

    /**
     * @return the shell to use when executing on unix., example "/bin/bash -E"
     */
    public String getUnixShell() {
        return unixShell;
    }

    /**
     * @param unixShell set the shell to use when executing on windows.
     */
    public void setUnixShell(String unixShell) {
        this.unixShell = unixShell;
    }

    /**
     * @param windowsShell the windows shell to use when executing on windows.
     */
    public void setWindowsShell(String windowsShell) {
        this.windowsShell = windowsShell;
    }

    /**
     * @return the windows shell to use example "powershell.exe -Command".
     */
    public String getWindowsShell() {
        return windowsShell;
    }
}
