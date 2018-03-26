package com.codingchili.zapperflyasm.commandline;

import com.codingchili.zapperflyasm.controller.*;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import io.vertx.core.*;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.util.*;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.context.*;
import com.codingchili.core.listener.BusRouter;
import com.codingchili.core.listener.ListenerSettings;
import com.codingchili.core.listener.transport.RestListener;

import static com.codingchili.core.files.Configurations.system;

/**
 * @author Robin Duda
 */
public class StartCommand implements Command {
    private ZapperConfig config = ZapperConfig.get();
    private static final String WEBSITE = "--website";
    private static final String PORT = "--port";
    private static final String DEFAULT_PORT = "443";
    private static final String NAME = "--name";
    private static final String GROUP = "--group";
    private static final String CAPACITY = "--capacity";

    @Override
    public void execute(Future<Boolean> start, CommandExecutor executor) {
        loadInstanceName(executor);
        loadInstanceGroup(executor);
        loadCapacity(executor);

        VertxOptions options = system().getOptions();

        HazelcastClusterManager hazel = new HazelcastClusterManager();
        Config config = new Config();
        GroupConfig group = new GroupConfig().setName(executor.getProperty(GROUP).orElse("default"));
        config.setGroupConfig(group);
        hazel.setConfig(config);
        options.setClusterManager(hazel);
        system().setOptions(options);

        SystemContext.clustered(clustered -> {
            CoreContext core = clustered.result();
            List<Future> deployments = new ArrayList<>();

            if (executor.hasProperty(WEBSITE) || executor.hasProperty(PORT)) {
                deployments.add(core.service(() -> new Webserver(
                        Integer.parseInt(executor.getProperty(PORT)
                                .orElse(DEFAULT_PORT)))));
            }

            deployments.add(core.handler(BuildHandler::new));
            deployments.add(core.listener(() ->
                    new RestListener()
                            .handler(new BusRouter())
                            .settings(ListenerSettings::new)));

            CompositeFuture.all(deployments).setHandler(done -> {
                start.complete(true);
            });
        });
    }

    private void loadCapacity(CommandExecutor executor) {
        config.setCapacity(Integer.parseInt(executor.getProperty(CAPACITY).orElse("2")));
    }

    private void loadInstanceGroup(CommandExecutor executor) {
        config.setGroupName(executor.getProperty(GROUP).orElse("default"));
    }

    private void loadInstanceName(CommandExecutor executor) {
        if (config.getInstanceName() == null) {
            config.setInstanceName(executor.getProperty(NAME)
                    .orElse(Environment.hostname()
                            .orElse(UUID.randomUUID().toString().split("-")[0])));
        }
    }

    @Override
    public String getDescription() {
        return "--website starts with the website on the default port " + DEFAULT_PORT + "\n" +
                "\t\t--port starts the website on the given port.\n"+
                "\t\t--name specifies the instance name.\n" +
                "\t\t--group specifies the cluster grouping.";
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static void main(String[] args) {
        System.out.println(args.length);

        new DefaultCommandExecutor()
                .add(new HelpCommand())
                .add(new StartCommand()).execute(args);
    }
}
