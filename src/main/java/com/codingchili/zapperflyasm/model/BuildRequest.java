package com.codingchili.zapperflyasm.model;

import javax.security.auth.login.Configuration;

import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.Request;
import com.codingchili.core.listener.RequestWrapper;
import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 * <p>
 * Maps requests to ease retrieval of attributes in the API.
 */
public class BuildRequest extends RequestWrapper {
    public static final String ID_BUILD = "id";
    public static final String ID_CONFIG = "config";
    public static final String ID_REPO = "repository";
    public static final String ID_BRANCH = "branch";
    public static final String ID_LOG = "log";
    public static final String ID_OFFSET = "offset";
    public static final String ID_LIST= "list";
    public static final String ID_TIME = "time";

    /**
     * @param request the original request to decorate.
     */
    public BuildRequest(Request request) {
        super(request);
    }

    /**
     * @return the id of the build the request regards.
     */
    public String getBuildId() {
        return data().getString(ID_BUILD);
    }

    /**
     * @return the line number offset for log data to be returned.
     */
    public int getLogOffset() {
        String offset = data().getString(ID_OFFSET);

        if (offset == null) {
            return 0;
        } else {
            return Integer.parseInt(offset);
        }
    }

    /**
     * @return a configuration object from the client if present.
     */
    public BuildConfiguration getConfiguration() {
        BuildConfiguration config = Serializer.unpack(data().getJsonObject(ID_CONFIG), BuildConfiguration.class);

        if (config.getBranch().isEmpty() || config.getRepository().isEmpty()) {
            throw new CoreRuntimeException("build or branch cannot be null.");
        }
        return config;
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
}
