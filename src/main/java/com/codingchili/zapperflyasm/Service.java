package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.commandline.HelpCommand;
import com.codingchili.zapperflyasm.commandline.StartCommand;
import io.vertx.core.Future;

import com.codingchili.core.Launcher;
import com.codingchili.core.context.DefaultCommandExecutor;
import com.codingchili.core.context.LaunchContext;
import com.codingchili.core.listener.CoreService;

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

        launcher().setVersion("1.0.0")
                .setApplication("zapperfly-asm")
                .deployable(Service.class)
                .setClustered(true);

        LaunchContext context = new LaunchContext(args);

        context.setCommandExecutor(new DefaultCommandExecutor()
                .add(new HelpCommand())
                .add(new StartCommand()));

        context.start();
    }

    @Override
    public void start(Future<Void> start) {
        start.complete();
    }
}
