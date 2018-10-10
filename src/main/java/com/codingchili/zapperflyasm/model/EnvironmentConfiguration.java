package com.codingchili.zapperflyasm.model;

import java.io.File;

import com.codingchili.core.security.SecretFactory;

/**
 * @author Robin Duda
 *
 * Environment specific configuration for the zapperfly server.
 */
public class EnvironmentConfiguration {
    private Integer timeoutSeconds = 300;
    private String tokenSecret = SecretFactory.generate(48);
    private String buildPath = new File("").toPath().toAbsolutePath().toString().replaceAll("\\\\", "/") + "/builds/";
    private String dockerLine = "docker run -w /tmp/build/ -v '$directory:/tmp/build' --rm $image /tmp/build/$script";
    private String groupName = "zapperfly-builds";
    private String windowsShell = "powershell.exe -Command";
    private String unixShell = "/bin/sh -c";
    private String instanceName = null;
    private int capacity = 2;

    /**
     * @return the timeout in seconds per build.
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
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

    /**
     * @return the shell to use when executing on unix., example "/bin/bash -c"
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

    /**
     * @return random bytes used for generating tokens.
     */
    public String getTokenSecret() {
        return tokenSecret;
    }

    /**
     * @param tokenSecret the secret to use when generating tokens.
     */
    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    /**
     * @return the maximum number of concurrent builds supported.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @param capacity the number of concurrent builds.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
