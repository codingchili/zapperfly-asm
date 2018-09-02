package com.codingchili.zapperflyasm.integration.bitbucket;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.logging.BuildEventListener;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.ZapperConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

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
        updateStatus(job);
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

            context.vertx().createHttpClient()
                    .post(bitbucket.getPort(),
                            bitbucket.getHost(),
                            bitbucket.getApi() + job.getCommit(),
                            response -> {
                                job.log(String.format("BitBucket %s server responded with '%d - %s' to status update.",
                                        getClass().getSimpleName(),
                                        response.statusCode(),
                                        response.statusMessage()));

                                response.bodyHandler(body -> job.log(body.toString()));
                            })
                    .putHeader(HttpHeaderNames.AUTHORIZATION, "Basic " + authentication)
                    .end(Serializer.buffer(new StatusUpdate(job, instances)));
        });

        logStatusUpdate(job);
    }

    private void logStatusUpdate(BuildJob job) {
        job.log(
                String.format("BitBucket %s updated status of build '%s' to %s ..",
                        getClass().getSimpleName(),
                        job.getId(),
                        job.getProgress().name()
                )
        );
    }
}
