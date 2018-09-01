package com.codingchili.zapperflyasm.commandline;

import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.handler.*;
import com.codingchili.zapperflyasm.integration.jenkins.WebhookNotifyCommit;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import io.vertx.core.*;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.*;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.context.*;
import com.codingchili.core.listener.MultiHandler;

import static com.codingchili.core.files.Configurations.system;

/**
 * @author Robin Duda
 * <p>
 * Command for starting the application and parsing some commandline options.
 */
public class StartCommand implements Command {
    private ZapperConfig config = ZapperConfig.get();
    private static final String WEBSITE = "--website";
    private static final String DEFAULT_PORT = "443";
    private static final String NAME = "--name";
    private static final String GROUP = "--group";
    private static final String CAPACITY = "--capacity";
    private EnvironmentConfiguration environment;

    @Override
    public void execute(Future<CommandResult> start, CommandExecutor executor) {
        environment = config.getEnvironment();
        loadInstanceName(executor);
        loadInstanceGroup(executor);
        loadCapacity(executor);

        VertxOptions options = system().getOptions();

        HazelcastClusterManager hazel = new HazelcastClusterManager();
        Config config = hazel.loadConfig();
        GroupConfig group = new GroupConfig().setName(executor.getProperty(GROUP).orElse("default"));
        config.setGroupConfig(group);
        options.setClusterManager(hazel);
        system().setOptions(options);

        SystemContext.clustered(clustered -> {
            if (clustered.succeeded()) {
                ZapperContext.get(clustered.result()).setHandler(done -> {
                    if (done.succeeded()) {
                        startup(start, executor, done.result());
                    } else {
                        start.fail(done.cause());
                    }
                });
            } else {
                start.fail(clustered.cause());
            }
        });
    }

    private void startup(Future<CommandResult> start, CommandExecutor executor, ZapperContext context) {
        List<Future> deployments = new ArrayList<>();
        MultiHandler api = new MultiHandler(
                new WebhookNotifyCommit(),
                new BuildHandler(),
                new ConfigurationHandler(),
                new AuthenticationHandler());

        if (executor.hasProperty(WEBSITE)) {
            int port = Integer.parseInt(executor.getProperty(WEBSITE)
                    .orElse(DEFAULT_PORT));

            InstanceInfo.get().setWebsiteEnabled(port);

            deployments.add(context.listener(() -> new Webserver(port)
                    .handler(api)));
        } else {
            deployments.add(context.handler(() -> api));
        }

        CompositeFuture.all(deployments).setHandler(deploy -> {
            if (deploy.succeeded()) {
                start.complete(CommandResult.STARTED);
            } else {
                start.fail(deploy.cause());
                context.close();
            }
        });
    }

    private void loadCapacity(CommandExecutor executor) {
        environment.setCapacity(Integer.parseInt(executor.getProperty(CAPACITY).orElse(
                (environment.getCapacity() + "")
        )));
    }

    private void loadInstanceGroup(CommandExecutor executor) {
        environment.setGroupName(executor.getProperty(GROUP).orElse("default"));
    }

    private void loadInstanceName(CommandExecutor executor) {
        if (environment.getInstanceName() == null) {
            environment.setInstanceName(executor.getProperty(NAME)
                    .orElse(Environment.hostname()
                            .orElse("zapperfly." + UUID.randomUUID().toString().split("-")[0])));
        }
    }

    @Override
    public String getDescription() {
        return "--start starts the application with options" +
                "\n\t\t--website starts with the website on the default port " + DEFAULT_PORT + "\n" +
                "\t\t--name specifies the instance name.\n" +
                "\t\t--group specifies the cluster grouping.";
    }

    @Override
    public String getName() {
        return "--start";
    }
}
