package com.codingchili.zapperflyasm.controller;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.function.Supplier;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.listener.*;
import com.codingchili.core.listener.transport.RestRequest;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * Used to serve the frontend and build artifacts :)
 */
public class Webserver implements CoreListener {
    private static final String POLYMER = "polymer/";
    private ListenerSettings settings = new ListenerSettings();
    private Logger logger;
    private CoreContext core;
    private CoreHandler handler;
    private int port;

    /**
     * @param port the port to start the webserver on.
     */
    public Webserver(Integer port) {
        this.port = port;
    }

    @Override
    public CoreListener settings(Supplier<ListenerSettings> settings) {
        this.settings = settings.get();
        return this;
    }

    @Override
    public CoreListener handler(CoreHandler handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public void init(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
        handler.init(core);
    }

    @Override
    public void start(Future<Void> start) {
        Router router = Router.router(core.vertx());
        router.route().handler(BodyHandler.create());

        router.route("/api/*").handler(route -> {
           handler.handle(new RestRequest(route, settings));
        });

        router.route("/*").handler(StaticHandler.create()
                .setCachingEnabled(false)
                .setWebRoot(POLYMER));

        core.vertx().createHttpServer(settings.getHttpOptions(core))
                .requestHandler(router::accept)
                .listen(port, (done) -> {
                    if (done.succeeded()) {
                        logger.log("webserver started on port " + port + ".");
                        handler.start(start);
                    } else {
                        start.fail(done.cause());
                    }
                });
    }
}
