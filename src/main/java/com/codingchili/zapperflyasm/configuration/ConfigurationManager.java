package com.codingchili.zapperflyasm.configuration;

import com.codingchili.zapperflyasm.model.BuildConfiguration;
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
    Future<Void> configure(BuildConfiguration config);

    /**
     * Removes configuration for the given repo and branch.
     *
     * @param id of the configuration to remove.
     */
    Future<Void> removeById(String id);

    /**
     * Retrieves configuration for the given repo and branch.
     *
     * @param id of the configuration to use for executing.
     * @return build configuration for the given combination of repo and branch.
     */
    Future<BuildConfiguration> retrieveById(String id);

    /**
     * Lists all available configurations.
     *
     * @return all configurations that exists.
     */
    Future<Collection<BuildConfiguration>> retrieveAll();

    /**
     * Lists all configurations for the given repository and branch.
     *
     * @param repository the repository the configuration is valid for.
     * @param branch the branch the configuration is valid for.
     * @return all configurations that matches the query.
     */
    Future<Collection<BuildConfiguration>> retrieveByQuery(String repository, String branch);
}
