package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.Future;

import java.util.Arrays;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.protocol.RoleMap.PUBLIC;

/**
 * @author Robin Duda
 *
 * Handler for repo/branch configurations.
 */
@Roles(PUBLIC)
@Address("config")
public class ConfigurationHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private ConfigurationManager configurations;

    @Override
    public void init(CoreContext core) {
        this.configurations = ZapperContext.ensure(core).getConfigurationManager();
    }

    @Override
    public void start(Future<Void> start) {
        // add a default test job.
        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("https://github.com/codingchili/zapperfly-asm.git");
        config.setBranch("master");
        config.setOutputDirs(Arrays.asList("out", "build", "target"));
        config.setCmdLine("./gradlew build --info --debug");
        config.setAutoclean(true);
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

    @Api
    @Description("Lists available configurations.")
    public void list(ApiRequest request) {
        configurations.getAllConfigs().setHandler(request::result);
    }

    @Api
    @Description("Lists the cluster configuration.")
    public void cluster(ApiRequest request) {
        request.write(ZapperConfig.get());
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(new ApiRequest(request));
    }
}
