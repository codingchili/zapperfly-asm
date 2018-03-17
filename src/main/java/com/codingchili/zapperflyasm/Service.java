package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.Arrays;

import com.codingchili.core.context.*;
import com.codingchili.core.files.Configurations;
import com.codingchili.core.listener.CoreService;
import com.codingchili.core.listener.ListenerSettings;
import com.codingchili.core.listener.transport.RestListener;
import com.codingchili.core.logging.Logger;

import static com.codingchili.core.files.Configurations.*;

/**
 * @author Robin Duda
 * <p>
 * Main zapperfly service to deploy the REST api and the website.
 */
public class Service implements CoreService {
    private ZapperConfig config;
    private CoreContext core;
    private Logger logger;

    @Override
    public void init(CoreContext core) {
        this.config = Configurations.get(ZapperConfig.PATH, ZapperConfig.class);
        this.core = core;
        this.logger = core.logger(getClass());
    }

    @Override
    public void start(Future<Void> start) {
        CompositeFuture.all(Arrays.asList(
                core.service(Webserver::new),
                core.listener(() -> new RestListener()
                        .handler(new BuildHandler())
                        .settings(ListenerSettings::new))
                        .setHandler(FutureHelper.generic(start))));
    }

    public static void main(String[] args) {
        system().setHandlers(1).setListeners(1).setServices(1).setMetrics(false);
        storage().setMaxResults(64);
        launcher().setVersion("1.0.0").setApplication("zapperfly-asm")
                .deployable(Service.class)
                .setClustered(false);

        new LaunchContext(args).start();
    }

}
