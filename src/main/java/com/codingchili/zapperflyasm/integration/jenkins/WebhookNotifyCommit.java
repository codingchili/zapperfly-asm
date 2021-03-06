package com.codingchili.zapperflyasm.integration.jenkins;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.protocol.RoleMap.USER;

/**
 * @author Robin Duda
 * <p>
 * Handles triggering of builds that uses the jenkins webhook format.
 * <p>
 * example: HTTP/1.1 GET /jenkins/git/notifyCommit?branches=master&repository=url-to-repo
 * <p>
 * - This requires an existing config for the branch and repository.
 * - The server triggering the build must have its hostname added to the whitelist.
 */
@Roles(USER)
@Address("jenkins")
public class WebhookNotifyCommit implements CoreHandler {
    private static final String BRANCHES = "branches";
    private static final String URL = "url";
    private Protocol<Request> protocol = new Protocol<>(this);
    private WebhookConfiguration config;
    private ZapperContext context;

    @Override
    public void init(CoreContext core) {
        context = ZapperContext.ensure(core);
        config = ZapperConfig.get()
                .getConfigurationByPlugin(getClass(), WebhookConfiguration.class);

        // simple authenticator that checks source IP.
        protocol.authenticator(request -> {
            String remote = request.connection().remote();
            Set<String> whitelist = config.getWhitelist();

            if (whitelist.contains(remote)) {
                return Future.succeededFuture(Role.USER);
            } else {
                return Future.failedFuture(new CoreRuntimeException(
                        String.format("The source IP '%s' is not in the jenkins whitelist '%s'.",
                                remote,
                                whitelist.stream().collect(Collectors.joining(", ")))));
            }
        });
    }

    @Api(route = "git/notifyCommit")
    public void notifyCommit(Request request) {
        JsonObject data = request.data();
        BuildConfiguration build = new BuildConfiguration();
        build.setRepository(data.getString(URL));
        build.setBranch(data.getString(BRANCHES));

        context.getConfigurationManager()
                .retrieveByQuery(
                        data.getString(URL),
                        data.getString(BRANCHES))
                .setHandler(done -> {
                    if (done.succeeded()) {
                        Collection<BuildConfiguration> configs = done.result();
                        // start all configurations for the given branch.
                        // more logic here would be nice, is tag, is marked for api etc.
                        configs.forEach(config -> context.getJobManager()
                                .submit(new BuildJob().setConfig(config))
                                .setHandler(request::result));
                    } else {
                        request.error(done.cause());
                    }
                });
    }

    @Override
    public void handle(Request request) {
        protocol.process(request);
    }
}
