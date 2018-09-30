package com.codingchili.zapperflyasm.configuration;

import com.codingchili.zapperflyasm.model.BuildConfiguration;
import io.vertx.core.Future;

import java.util.Collection;

import com.codingchili.core.storage.AsyncStorage;

import static com.codingchili.zapperflyasm.model.ApiRequest.ID_BRANCH;
import static com.codingchili.zapperflyasm.model.ApiRequest.ID_REPO;

/**
 * @author Robin Duda
 * <p>
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
    public Future<Void> removeById(String configId) {
        Future<Void> future = Future.future();
        configurations.remove(configId, future);
        return future;
    }

    @Override
    public Future<BuildConfiguration> retrieveById(String configId) {
        Future<BuildConfiguration> future = Future.future();
        configurations.get(configId, future);
        return future;
    }

    @Override
    public Future<Collection<BuildConfiguration>> retrieveAll() {
        Future<Collection<BuildConfiguration>> future = Future.future();
        configurations.values(future);
        return future;
    }

    @Override
    public Future<Collection<BuildConfiguration>> retrieveByQuery(String repository, String branch) {
        Future<Collection<BuildConfiguration>> future = Future.future();

        configurations
                .query(ID_REPO).equalTo(repository)
                .and(ID_BRANCH).equalTo(branch)
                .execute(future);

        return future;
    }
}
