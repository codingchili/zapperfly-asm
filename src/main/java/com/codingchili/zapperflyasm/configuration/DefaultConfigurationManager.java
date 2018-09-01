package com.codingchili.zapperflyasm.configuration;

import com.codingchili.zapperflyasm.model.BuildConfiguration;
import io.vertx.core.Future;

import java.util.Collection;

import com.codingchili.core.storage.AsyncStorage;

/**
 * @author Robin Duda
 *
 * Manages the branch/repo configurations for building.
 */
public class DefaultConfigurationManager implements ConfigurationManager {
    private AsyncStorage<BuildConfiguration> configurations;

    /**
     * Creates a new configuration manager.
     *
     * @param configs the storage implementation to use for the configuration.
     */
    public DefaultConfigurationManager(AsyncStorage<BuildConfiguration> configs) {
        this.configurations = configs;
    }

    @Override
    public Future<Void> configure(BuildConfiguration config) {
        Future<Void> future = Future.future();
        configurations.put(config, future);
        return future;
    }

    @Override
    public Future<Void> removeByRepositoryAndBranch(String repository, String branch) {
        Future<Void> future = Future.future();
        configurations.remove(BuildConfiguration.toKey(repository, branch), future);
        return future;
    }

    @Override
    public Future<BuildConfiguration> retrieveByRepositoryAndBranch(String repository, String branch) {
        Future<BuildConfiguration> future = Future.future();
        configurations.get(BuildConfiguration.toKey(repository, branch), future);
        return future;
    }

    @Override
    public Future<Collection<BuildConfiguration>> retrieveAll() {
        Future<Collection<BuildConfiguration>> future = Future.future();
        configurations.values(future);
        return future;
    }
}
