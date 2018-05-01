package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;
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
        this.configurations = ZapperContext.ensure(core).getConfigurationManager();
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
        configurations.putConfig(config).setHandler(start);
    }

    @Api
    @Description("Adds configuration for the given repository and branch.")
    public void add(ApiRequest request) {
        configurations.putConfig(request.getConfiguration()).setHandler(request::result);
    }

    @Api
    @Description("Removes configuration for the given repository and branch.")
    public void remove(ApiRequest request) {
        configurations.removeConfig(request.getRepository(), request.getBranch()).setHandler(request::result);
    }

    @Api(USER)
    @Description("Lists available configurations.")
    public void list(ApiRequest request) {
        configurations.getAllConfigs().setHandler(request::result);
    }

    @Api(USER)
    @Description("Lists the cluster configuration.")
    public void cluster(ApiRequest request) {
        request.write(ZapperConfig.get());
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route(), core.authenticator().getRoleByRequest(request))
                .submit(new ApiRequest(request));
    }
}
