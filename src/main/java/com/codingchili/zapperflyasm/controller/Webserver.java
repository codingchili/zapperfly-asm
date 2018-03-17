package com.codingchili.zapperflyasm.controller;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.FutureHelper;
import com.codingchili.core.listener.CoreService;
import com.codingchili.core.listener.ListenerSettings;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 *
 * Used to serve the frontend and build artifacts :)
 */
public class Webserver implements CoreService {
    private static final String POLYMER = "polymer/";
    private Logger logger;
    private CoreContext core;

    @Override
    public void init(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
    }

    @Override
    public void start(Future<Void> start) {
        Router router = Router.router(core.vertx());
        router.route().handler(BodyHandler.create());

        router.route("/*").handler(StaticHandler.create()
                .setCachingEnabled(false)
                .setWebRoot(POLYMER));

        core.vertx().createHttpServer(new ListenerSettings().getHttpOptions(core))
                .requestHandler(router::accept)
                .listen(443, FutureHelper.generic(start));

        logger.log("hello service started.");
    }
}
