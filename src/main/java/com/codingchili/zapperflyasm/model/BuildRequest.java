package com.codingchili.zapperflyasm.model;

import com.codingchili.core.listener.Request;
import com.codingchili.core.listener.RequestWrapper;
import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 *
 * Maps requests to ease retrieval of attributes in the API.
 */
public class BuildRequest extends RequestWrapper {

    public BuildRequest(Request request) {
        super(request);
    }

    public String getBuildId() {
        return data().getString("build");
    }

    public int getLogOffset() {
        return data().getInteger("log");
    }

    public BuildConfiguration getConfiguration() {
        return Serializer.unpack(data().getJsonObject("config"), BuildConfiguration.class);
    }

    public String getRepository() {
        return data().getString("repository");
    }

    public String getBranch() {
        return data().getString("branch");
    }
}
