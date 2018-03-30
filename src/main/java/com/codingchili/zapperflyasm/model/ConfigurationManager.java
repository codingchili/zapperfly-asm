package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;

import java.util.Collection;

/**
 * @author Robin Duda
 *
 * Interface for build configuration management.
 */
public interface ConfigurationManager {

    /**
     * Adds configuration for a repo and branch combination.
     *
     * @param config the configuration to add.
     */
    Future<Void> putConfig(BuildConfiguration config);

    /**
     * Removes configuration for the given repo and branch.
     *
     * @param repository the repo to remove configuration for.
     * @param branch     the branch to remove configuration for.
     */
    Future<Void> removeConfig(String repository, String branch);

    /**
     * Retrieves configuration for the given repo and branch.
     *
     * @param repository the repository to retrieve config of.
     * @param branch     the branch to retrieve config of.
     * @return build configuration for the given combination of repo and branch.
     */
    Future<BuildConfiguration> getConfig(String repository, String branch);

    /**
     * Lists all available configurations.
     *
     * @return all configurations that exists.
     */
    Future<Collection<BuildConfiguration>> getAllConfigs();

}
