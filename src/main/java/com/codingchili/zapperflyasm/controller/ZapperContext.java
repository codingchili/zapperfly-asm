package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import com.codingchili.core.context.*;
import com.codingchili.core.storage.AsyncStorage;

/**
 * @author Robin Duda
 * <p>
 * Application specific context for the Zapperfly build server.
 */
public class ZapperContext extends SystemContext {
    private DefaultConfigurationManager configurations;
    private DefaultJobManager builds;

    /**
     * Creates the context asynchronously.
     *
     * @param core the core-context to use.
     * @return callback.
     */
    public static Future<ZapperContext> get(CoreContext core) {
        Future<ZapperContext> future = Future.future();

        CompositeFuture.join(
                ZapperConfig.getStorage(core, BuildJob.class),
                ZapperConfig.getStorage(core, BuildConfiguration.class),
                ZapperConfig.getStorage(core, LogEvent.class),
                ZapperConfig.getStorage(core, InstanceInfo.class)
        ).setHandler(done -> {
            if (done.succeeded()) {
                AsyncStorage<BuildJob> jobs = done.result().resultAt(0);
                AsyncStorage<BuildConfiguration> configs = done.result().resultAt(1);
                AsyncStorage<LogEvent> logs = done.result().resultAt(2);
                AsyncStorage<InstanceInfo> instances = done.result().resultAt(3);

                ZapperContext zapper = new ZapperContext();

                zapper.builds = new DefaultJobManager(core, jobs, logs, instances);
                zapper.configurations = new DefaultConfigurationManager(configs);

                future.complete(zapper);
            } else {
                future.fail(done.cause());
            }
        });
        return future;
    }

    /**
     * Asserts that the given context is a Zapperfly context.
     *
     * @param context the context to verify and cast.
     * @return the given context casted to a zapper context.
     */
    public static ZapperContext ensure(CoreContext context) {
        if (context instanceof ZapperContext) {
            return (ZapperContext) context;
        } else {
            throw new CoreRuntimeException("Assertion failed; expected ZapperContext got: " + context);
        }
    }

    /**
     * @return the job manager to use for the application context.
     */
    public JobManager getJobManager() {
        return builds;
    }

    /**
     * @return the configuration manager to use for the application context.
     */
    public ConfigurationManager getConfigurationManager() {
        return configurations;
    }
}
