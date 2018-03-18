package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.BuildHandler;
import com.codingchili.zapperflyasm.controller.Webserver;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.Arrays;

import com.codingchili.core.context.*;
import com.codingchili.core.listener.*;
import com.codingchili.core.listener.transport.RestListener;

import static com.codingchili.core.files.Configurations.*;

/**
 * @author Robin Duda
 * <p>
 * Main zapperfly service to deploy the REST api and the website.
 */
public class Service implements CoreService {
    private CoreContext core;

    @Override
    public void init(CoreContext core) {
        this.core = core;
    }

    @Override
    public void start(Future<Void> start) {
        CompositeFuture.all(Arrays.asList(
                core.service(Webserver::new),
                core.handler(BuildHandler::new),
                core.listener(() -> new RestListener()
                        .handler(new BusRouter())
                        .settings(ListenerSettings::new))
                        .setHandler(FutureHelper.generic(start))));
    }

    public static void main(String[] args) {
        system().setHandlers(4)
                .setListeners(1)
                .setServices(1)
                .setMetrics(false);

        launcher().setVersion("1.0.0")
                .setApplication("zapperfly-asm")
                .deployable(Service.class)
                .setClustered(true);

        new LaunchContext(args).start();
    }
}
