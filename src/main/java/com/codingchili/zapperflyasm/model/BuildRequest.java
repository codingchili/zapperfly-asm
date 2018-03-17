package com.codingchili.zapperflyasm.model;

import com.codingchili.core.listener.Request;
import com.codingchili.core.listener.RequestWrapper;
import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 * <p>
 * Maps requests to ease retrieval of attributes in the API.
 */
public class BuildRequest extends RequestWrapper {

    public BuildRequest(Request request) {
        super(request);
    }

    /**
     * @return the id of the build the request regards.
     */
    public String getBuildId() {
        return data().getString("build");
    }

    /**
     * @return the line number offset for log data to be returned.
     */
    public int getLogOffset() {
        return data().getInteger("log");
    }

    /**
     * @return a configuration object from the client if present.
     */
    public BuildConfiguration getConfiguration() {
        return Serializer.unpack(data().getJsonObject("config"), BuildConfiguration.class);
    }

    /**
     * @return the repository the request regards.
     */
    public String getRepository() {
        return data().getString("repository");
    }

    /**
     * @return the branch to execute on.
     */
    public String getBranch() {
        return data().getString("branch");
    }
}
