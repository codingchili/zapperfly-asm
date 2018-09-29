package com.codingchili.zapperflyasm.model;

import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.Request;
import com.codingchili.core.listener.RequestWrapper;
import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 * <p>
 * Maps requests to ease retrieval of attributes in the API.
 */
public class ApiRequest implements RequestWrapper {
    public static final String ID = "id";
    public static final String ID_CONFIG = "config";
    public static final String ID_REPO = "repository";
    public static final String ID_BRANCH = "branch";
    public static final String ID_LOG = "log";
    public static final String ID_OFFSET = "offset";
    public static final String ID_LIST = "list";
    public static final String ID_TIME = "time";
    public static final String ID_DIRECTORY = "directory";
    private Request request;

    /**
     * @param request the original request to decorate.
     */
    public ApiRequest(Request request) {
        this.request = request;
    }

    /**
     * @return the id of the build the request regards.
     */
    public String getBuildId() {
        return data().getString(ID);
    }

    /**
     * @return the id of a configuration.
     */
    public String getConfigId() {
        return data().getString(ID);
    }

    /**
     * @return the line number offset for log data to be returned.
     */
    public Long getLogOffset() {
        Long offset = data().getLong(ID_OFFSET);

        if (offset == null) {
            return 0L;
        } else {
            return offset;
        }
    }

    /**
     * @return a configuration object from the client if present.
     */
    public BuildConfiguration getConfiguration() {
        BuildConfiguration config = Serializer.unpack(data().getJsonObject(ID_CONFIG), BuildConfiguration.class);

        config.sanitize();

        require("repository", config.getRepository());
        require("branch", config.getBranch());
        require("cmdline", config.getCmdLine());

        return config;
    }

    private void require(String name, String value) {
        if (value == null || value.isEmpty()) {
            throw new CoreRuntimeException(name + " is required.");
        }
    }

    /**
     * @return the repository the request regards.
     */
    public String getRepository() {
        return data().getString(ID_REPO);
    }

    /**
     * @return the branch to execute on.
     */
    public String getBranch() {
        return data().getString(ID_BRANCH);
    }

    @Override
    public Request request() {
        return request;
    }
}
