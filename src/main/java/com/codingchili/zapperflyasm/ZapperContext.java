package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.building.BuildManager;
import com.codingchili.zapperflyasm.building.DefaultBuildManager;
import com.codingchili.zapperflyasm.configuration.ConfigurationManager;
import com.codingchili.zapperflyasm.configuration.DefaultConfigurationManager;
import com.codingchili.zapperflyasm.building.ProcessBuilderExecutor;
import com.codingchili.zapperflyasm.handler.Authenticator;
import com.codingchili.zapperflyasm.logging.HazelLogStore;
import com.codingchili.zapperflyasm.logging.LogStore;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.scheduling.HazelJobQueue;
import com.codingchili.zapperflyasm.vcs.GitExecutor;
import com.codingchili.zapperflyasm.vcs.VersionControlSystem;
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
    private ProcessBuilderExecutor executor;
    private Authenticator authenticator;
    private VersionControlSystem vcs;
    private BuildManager builder;
    private LogStore logStore;


    /**
     * @param core an existing context.
     */
    public ZapperContext(CoreContext core) {
        super(core);
    }

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
                ZapperConfig.getStorage(core, InstanceInfo.class)
        ).setHandler(done -> {
            if (done.succeeded()) {
                AsyncStorage<BuildJob> builds = done.result().resultAt(0);
                AsyncStorage<BuildConfiguration> configs = done.result().resultAt(1);
                AsyncStorage<InstanceInfo> instances = done.result().resultAt(2);

                ZapperContext zapper = new ZapperContext(core);
                DefaultBuildManager manager = new DefaultBuildManager(core);

                manager.setInstances(instances);
                manager.setBuilds(builds);

                zapper.vcs = new GitExecutor(core);
                zapper.executor = new ProcessBuilderExecutor(core);

                HazelJobQueue.create(core).setHandler(queue -> {
                    if (queue.succeeded()) {
                        manager.setQueue(queue.result());
                    }
                });
                HazelLogStore.create(core).setHandler(logStore -> {
                    if (logStore.succeeded()) {
                        zapper.logStore = logStore.result();
                    }
                });

                zapper.configurations = new DefaultConfigurationManager(configs);
                zapper.builder = manager;
                manager.start();

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
    public BuildManager getJobManager() {
        return builder;
    }

    /**
     * @return the configuration manager to use for the application context.
     */
    public ConfigurationManager getConfigurationManager() {
        return configurations;
    }

    /**
     * @return the logstore to be used for logging and event management.
     */
    public LogStore getLogStore() {
        return logStore;
    }

    /**
     * @return get the version control system implementation.
     */
    public VersionControlSystem getVcs() {
        return vcs;
    }

    /**
     * @return get the process builder for executing system processes.
     */
    public ProcessBuilderExecutor getExecutor() {
        return executor;
    }

    /**
     * @return the authenticator responsible for adding users, signing tokens
     * adding new users etc.
     */
    public Authenticator authenticator() {
        if (authenticator == null) {
            authenticator = new Authenticator(this);
        }
        return authenticator;
    }
}
