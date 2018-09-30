package com.codingchili.zapperflyasm.integration.bitbucket;

/**
 * @author Robin Duda
 *
 * Configuration for uploading build status to bitbucket server.
 */
public class BitbucketConfiguration {
    private boolean ssl = true;
    private boolean debug = false;
    private String user;
    private String pass;
    private String api;
    private String host;
    private int port;

    /**
     * @return the username that will be used to authenticate against the REST api.
     */
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password of the username that will be used to authenticate against the REST api.
     */
    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * @return the REST API endpoint for the bitbucket server.
     */
    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    /**
     * @return hostname of the API server.
     */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return port that the REST service is running on.
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return true if TLS must be used for the connection.
     */
    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * @return if true emits additional information into the build log.
     */
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
