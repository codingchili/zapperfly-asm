package com.codingchili.zapperflyasm.handler;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.model.BuildConfiguration;
import com.codingchili.zapperflyasm.configuration.ConfigurationManager;
import io.vertx.core.Future;

import java.util.Arrays;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.protocol.RoleMap.ADMIN;
import static com.codingchili.core.protocol.RoleMap.USER;

/**
 * @author Robin Duda
 * <p>
 * Handler for repo/branch configurations.
 */
@Roles(ADMIN)
@Address("config")
public class ConfigurationHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private ConfigurationManager configurations;
    private ZapperContext core;

    @Override
    public void init(CoreContext core) {
        this.core = ZapperContext.ensure(core);
        this.configurations = this.core.getConfigurationManager();
    }

    @Override
    public void start(Future<Void> start) {
        // add a default test job.
        BuildConfiguration config = new BuildConfiguration()
                .setRepository("https://github.com/octocat/Hello-World.git")
                .setBranch("master")
                .setOutputDirs(Arrays.asList("out", "build", "target"))
                .setCmdLine("java -version")
                .setAutoclean(true);
        configurations.configure(config).setHandler(start);
    }

    @Api
    @Description("Adds configuration for the given repository and branch.")
    public void add(ApiRequest request) {
        configurations.configure(request.getConfiguration()).setHandler(request::result);
    }

    @Api
    @Description("Removes configuration for the given repository and branch.")
    public void remove(ApiRequest request) {
        configurations.removeByRepositoryAndBranch(request.getRepository(), request.getBranch()).setHandler(request::result);
    }

    @Api(USER)
    @Description("Lists available configurations.")
    public void list(ApiRequest request) {
        configurations.retrieveAll().setHandler(request::result);
    }

    @Api(USER)
    @Description("Lists the cluster configuration.")
    public void cluster(ApiRequest request) {
        request.write(ZapperConfig.getEnvironment());
    }

    @Override
    public void handle(Request request) {
        core.authenticator().getRoleByRequest(request).setHandler(done -> {
            protocol.get(request.route(), done.result())
                    .submit(new ApiRequest(request));
        });
    }
}
