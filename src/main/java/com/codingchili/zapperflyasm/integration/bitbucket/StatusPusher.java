package com.codingchili.zapperflyasm.integration.bitbucket;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.logging.BuildEventListener;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.ZapperConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.http.HttpClientOptions;

import java.util.Base64;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.listener.CoreService;
import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 * <p>
 * Listens for changes to build status and updates a bitbucket server.
 * <p>
 * - requires a username and password for authentication against the REST api.
 * - requires a configured bitbucket server host.
 */
public class StatusPusher implements BuildEventListener, CoreService {
    private BitbucketConfiguration bitbucket;
    private ZapperContext context;

    @Override
    public void init(CoreContext core) {
        context = ZapperContext.ensure(core);
        context.getLogStore().addListener(this);
        bitbucket = ZapperConfig.get().getConfigurationByPlugin(getClass(), BitbucketConfiguration.class);
    }

    @Override
    public void onBuildQueued(BuildJob job) {
        //updateStatus(job); -- cannot update the status here because the commit id is not available.
    }

    @Override
    public void onBuildStarted(BuildJob job) {
        updateStatus(job);
    }

    @Override
    public void onBuildComplete(BuildJob job) {
        updateStatus(job);
    }

    private void updateStatus(BuildJob job) {
        context.getJobManager().instances().setHandler(instances -> {

            String authentication = Base64.getEncoder()
                    .encodeToString(
                            (bitbucket.getUser() + ":" + bitbucket.getPass()).getBytes()
                    );

            context.vertx().createHttpClient(getHttpOptions())
                    .post(bitbucket.getPort(),
                            bitbucket.getHost(),
                            bitbucket.getApi() + job.getFullCommit(),
                            response -> {

                                if (response.statusCode() >= 400 || bitbucket.isDebug()) {
                                    job.log(String.format("BitBucket %s server responded with '%d - %s' to status update.",
                                            getClass().getSimpleName(),
                                            response.statusCode(),
                                            response.statusMessage()));
                                }

                                if (bitbucket.isDebug()) {
                                    response.bodyHandler(body -> job.log(body.toString()));
                                }
                            })
                    .putHeader(HttpHeaderNames.AUTHORIZATION, "Basic " + authentication)
                    .end(Serializer.buffer(new StatusUpdate(job, instances)));
        });

        logStatusUpdate(job);
    }

    private HttpClientOptions getHttpOptions() {
        return new HttpClientOptions().setSsl(bitbucket.isSsl());
    }

    private void logStatusUpdate(BuildJob job) {
        job.log(
                String.format("BitBucket %s updating status of build '%s' to %s ..",
                        getClass().getSimpleName(),
                        job.getId(),
                        BitbucketBuildStatus.fromBuildStatus(job.getProgress()).name()
                )
        );
    }
}
