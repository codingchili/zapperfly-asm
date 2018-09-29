package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.building.*;
import com.codingchili.zapperflyasm.configuration.ConfigurationManager;
import com.codingchili.zapperflyasm.configuration.DefaultConfigurationManager;
import com.codingchili.zapperflyasm.logging.*;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.scheduling.JobQueueMock;
import com.codingchili.zapperflyasm.vcs.VCSMock;
import com.codingchili.zapperflyasm.vcs.VersionControlSystem;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import com.codingchili.core.configuration.Environment;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.context.SystemContext;
import com.codingchili.core.storage.*;

/**
 * @author Robin Duda
 * <p>
 * Mock for the zapper application context.
 */
public class ZapperContextMock extends ZapperContext {
    private ConfigurationManager branches;
    private DefaultBuildManager manager;
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<InstanceInfo> instances;
    private AsyncStorage<BuildJob> builds;
    private LogStoreMock logs;
    private BuildExecutor executor;
    private VersionControlSystem vcs;

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

        ZapperConfig.setStoragePlugin(JsonMap.class);
        ZapperConfig config = ZapperConfig.get();
        ZapperConfig.getEnvironment().setBuildPath("test/resources/");
        ZapperConfig.getEnvironment().setTimeoutSeconds(3);
        ZapperConfig.getEnvironment().setInstanceName(Environment.hostname().orElse("zap.instance.1"));

        mock.configs = getMap(BuildConfiguration.class);
        mock.builds = getMap(BuildJob.class);
        mock.instances = getMap(InstanceInfo.class);
        mock.logs = new LogStoreMock();
        mock.branches = new DefaultConfigurationManager(mock.configs);
        mock.executor = new BuildExecutorMock(true);
        mock.vcs = new VCSMock(mock);

        DefaultBuildManager manager = new DefaultBuildManager();

        manager.setQueue(new JobQueueMock());
        manager.setBuilds(mock.builds);
        manager.setInstances(mock.instances);

        manager.init(mock);
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
     * @param line adds a log event to the log store.
     */
    public void log(String line) {
        logs.add("default", line);
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

    @Override
    public LogStore getLogStore() {
        return logs;
    }

    @Override
    public VersionControlSystem getVcs() {
        return vcs;
    }

    @Override
    public BuildExecutor getExecutor() {
        return executor;
    }
}
