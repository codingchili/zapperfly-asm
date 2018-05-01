package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.commandline.*;
import io.vertx.core.Future;

import com.codingchili.core.Launcher;
import com.codingchili.core.context.DefaultCommandExecutor;
import com.codingchili.core.context.LaunchContext;
import com.codingchili.core.listener.CoreService;
import com.codingchili.core.protocol.RoleMap;

import static com.codingchili.core.files.Configurations.*;

/**
 * @author Robin Duda
 * <p>
 * Main zapperfly service to deploy the REST api and the website.
 */
public class Service implements CoreService {

    public static void main(String[] args) {
        system().setHandlers(1)
                .setListeners(1)
                .setServices(1)
                .setMetrics(false);

        launcher().setVersion("1.0.3")
                .setApplication("zapperfly-asm")
                .deployable(Service.class)
                .setWarnOnDefaultsLoaded(true)
                .setClustered(true);

        LaunchContext context = new LaunchContext(args);

        context.setCommandExecutor(new DefaultCommandExecutor()
                .add(new HelpCommand())
                .add(new ConfigureCommand())
                .add(new StartCommand())
                .add(new AddUserCommand()));

        context.start();
    }

    @Override
    public void start(Future<Void> start) {
        start.complete();
    }
}
