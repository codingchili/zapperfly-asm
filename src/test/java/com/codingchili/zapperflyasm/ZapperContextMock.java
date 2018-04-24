package com.codingchili.zapperflyasm;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.context.SystemContext;
import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.controller.ZapperContext;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.storage.*;

/**
 * @author Robin Duda
 *
 * Mock for the zapper application context.
 */
public class ZapperContextMock extends ZapperContext {
    private ConfigurationManager branches;
    private DefaultBuildManager manager;
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<InstanceInfo> instances;
    private AsyncStorage<BuildJob> builds;
    private LogStoreMock logs;

    public ZapperContextMock() {
        super(new SystemContext());
    }

    /**
     * Creates a new mock context asynchronously.
     *
     * @return callback.
     */
    public static Future<ZapperContextMock> create() {
        ZapperContextMock mock = new ZapperContextMock();

        ZapperConfig config = ZapperConfig.get();
        config.setStorage(JsonMap.class.getName());
        config.setBuildPath("test/resources/");
        config.setTimeoutSeconds(3);
        config.setInstanceName(Environment.hostname().orElse("zap.instance.1"));

        mock.configs = getMap(BuildConfiguration.class);
        mock.builds = getMap(BuildJob.class);
        mock.instances = getMap(InstanceInfo.class);
        mock.logs = new LogStoreMock();
        mock.branches = new DefaultConfigurationManager(mock.configs);

        DefaultBuildManager manager = new DefaultBuildManager(mock);

        manager.setExecutor(new BuildExecutorMock(true));
        manager.setQueue(new JobQueueMock());
        manager.setLogs(mock.logs);
        manager.setBuilds(mock.builds);
        manager.setVcs(new VCSMock(mock));
        manager.setInstances(mock.instances);

        mock.manager = manager;
        return Future.succeededFuture(mock);
    }

    private static <T extends Storable> JsonMap<T> getMap(Class<T> aClass) {
        return new JsonMap<>(Future.future(), new StorageContextMock<>(aClass));
    }

    @Override
    public BuildManager getJobManager() {
        return manager;
    }

    @Override
    public ConfigurationManager getConfigurationManager() {
        return branches;
    }

    /**
     * @param logEvent adds a log event to the log store.
     */
    public void log(LogEvent logEvent) {
        logs.add("default", logEvent);
    }

    /**
     * @param job adds a job to the job store.
     */
    public void addJob(BuildJob job) {
        builds.put(job, this::succeed);
    }

    public void addConfig(BuildConfiguration config) {
        configs.put(config, this::succeed);
    }

    private void succeed(AsyncResult<Void> result) {
        if (result.failed()) {
            throw new CoreRuntimeException(result.cause().getMessage());
        }
    }
}
