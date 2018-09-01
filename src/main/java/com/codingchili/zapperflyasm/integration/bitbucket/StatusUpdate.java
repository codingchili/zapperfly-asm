package com.codingchili.zapperflyasm.integration.bitbucket;

import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.InstanceInfo;
import io.vertx.core.AsyncResult;

import java.util.Collection;

/**
 * @author Robin Duda
 * <p>
 * API model object for updating the build status on a bitbucket server.
 */
public class StatusUpdate {
    private BitbucketBuildStatus state;
    private String key;
    private String name;
    private String url;
    private String description;

    public StatusUpdate(BuildJob job, AsyncResult<Collection<InstanceInfo>> result) {

        if (result.succeeded()) {
            url = result.result().stream()
                    .filter(instance -> instance.getWebserver() != null)
                    .map(instance -> String.format("https://%s:%d/?id=%s",
                            instance.getWebserver(), instance.getWebserverPort(), job.getId()))
                    .findFirst()
                    .orElse(null);
        }

        state = BitbucketBuildStatus.fromBuildStatus(job.getProgress());
        key = job.getId();
        name = job.getMessage();
        description = String.format("executing on zapperfly instance %s [%s]",
                job.getInstance(), job.getProgress());
    }

    public BitbucketBuildStatus getState() {
        return state;
    }

    public void setState(BitbucketBuildStatus state) {
        this.state = state;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
