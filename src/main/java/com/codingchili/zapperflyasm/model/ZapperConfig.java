package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.*;

import com.codingchili.core.configuration.Configurable;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.files.Configurations;
import com.codingchili.core.protocol.Serializer;
import com.codingchili.core.storage.*;

/**
 * @author Robin Duda
 * <p>
 * Contains configuration for the server.
 * <p>
 * Configuration needs only to exist on a single node :)
 * That node must be alive when other nodes are starting,
 * but can go offline without interrupting any services.
 */
public class ZapperConfig implements Configurable {
    private static String PATH = "./zapperfly.yaml";
    private static Class<? extends AsyncStorage> storage = HazelMap.class;
    private Set<User> users = new HashSet<>();
    private EnvironmentConfiguration environment;
    private JsonObject integrations;

    /**
     * Retrieves a storage implementation used to host objects of the given class.
     *
     * @param core  the core context to create the storage on.
     * @param value the class of the value to be stored.
     * @param <T>   the type of the value to be stored.
     * @return callback.
     */
    public static <T extends Storable> Future<AsyncStorage<T>> getStorage(CoreContext core,
                                                                          Class<T> value) {
        Future<AsyncStorage<T>> future = Future.future();
        new StorageLoader<T>(core)
                .withPlugin(storage)
                .withValue(value)
                .withDB(value.getSimpleName())
                .build(done -> {
                    if (done.succeeded()) {
                        future.complete(done.result());
                    } else {
                        future.fail(done.cause());
                    }
                });
        return future;
    }

    /**
     * @param plugin the plugin used to store builds.
     */
    public static void setStoragePlugin(Class<? extends AsyncStorage> plugin) {
        ZapperConfig.storage = plugin;
    }

    /**
     * @return a list of configured users.
     */
    public Set<User> getUsers() {
        return users;
    }

    /**
     * @param users a list of users.
     */
    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @Override
    public String getPath() {
        return PATH;
    }

    /**
     * @return the loaded configuration.
     */
    public static ZapperConfig get() {
        return Configurations.get(PATH, ZapperConfig.class);
    }

    /**
     * @return the environment specific configuration for the current instance.
     */
    public EnvironmentConfiguration getEnvironment() {
        return environment;
    }

    /**
     * @param environment the environment speicific configuration for the current instance.
     */
    public void setEnvironment(EnvironmentConfiguration environment) {
        this.environment = environment;
    }

    /**
     * Get the configuration of the specified integration.
     *
     * @param pluginClass the plugin class that the configuration is keyed as.
     * @return the configuration as json if available.
     */
    public <E> E getConfigurationByPlugin(Class pluginClass, Class<E> theClass) {
        if (integrations.containsKey(pluginClass.getName())) {
            return Serializer.unpack(integrations.getJsonObject(pluginClass.getName()), theClass);
        } else {
            try {
                return theClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return a json object that represents all of the integration specific configuration.
     */
    public JsonObject getIntegrations() {
        return integrations;
    }

    /**
     * @param json a jsonobject that contains configuration for integrations.
     */
    public void setIntegrations(JsonObject json) {
        integrations = json;
    }
}
