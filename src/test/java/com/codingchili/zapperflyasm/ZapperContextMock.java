package com.codingchili.zapperflyasm;

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
    private DefaultJobManager manager;
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<InstanceInfo> instances;
    private AsyncStorage<BuildJob> jobs;
    private AsyncStorage<LogEvent> logs;

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

        mock.configs = getMap(BuildConfiguration.class);
        mock.jobs = getMap(BuildJob.class);
        mock.logs = getMap(LogEvent.class);
        mock.instances = getMap(InstanceInfo.class);

        mock.manager = new DefaultJobManager(mock, mock.jobs, mock.logs, mock.instances);
        mock.branches = new DefaultConfigurationManager(mock.configs);
        mock.manager.setBuildExecutor(new BuildExecutorMock(true));
        mock.manager.setVCSProvider(new VCSMock(mock));
        return Future.succeededFuture(mock);
    }

    private static <T extends Storable> JsonMap<T> getMap(Class<T> aClass) {
        return new JsonMap<>(Future.future(), new StorageContextMock<>(aClass));
    }

    @Override
    public JobManager getJobManager() {
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
        logs.put(logEvent, this::succeed);
    }

    /**
     * @param job adds a job to the job store.
     */
    public void addJob(BuildJob job) {
        jobs.put(job, this::succeed);
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
