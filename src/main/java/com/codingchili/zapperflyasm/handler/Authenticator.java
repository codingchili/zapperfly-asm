package com.codingchili.zapperflyasm.handler;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import com.codingchili.zapperflyasm.model.User;
import io.vertx.core.Future;

import java.util.Optional;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.RoleMap;
import com.codingchili.core.protocol.RoleType;
import com.codingchili.core.security.*;

/**
 * @author Robin Duda
 * <p>
 * Handles authentication.
 */
public class Authenticator {
    private static final String ID_ROLE = "role";
    private TokenFactory factory;
    private HashFactory hasher;

    /**
     * @param core creates a new authenticator on the given core context.
     */
    public Authenticator(CoreContext core) {
        this.hasher = new HashFactory(core);
        this.factory = new TokenFactory(core, ZapperConfig.getEnvironment().getTokenSecret().getBytes());
    }

    /**
     * @return a token factory used to generate and verify tokens.
     */
    public Future<Token> generateToken(User user) {
        Future<Token> future = Future.future();
        Token token = new Token()
                .setDomain(user.getUsername())
                .addProperty(ID_ROLE, user.getRole());
        factory.hmac(token).setHandler(done -> {
            if (done.succeeded()) {
                future.complete(token);
            } else {
                future.fail(done.cause());
            }
        });
        return future;
    }

    /**
     * get the role of the token contained in the given request.
     *
     * @param request carries the token to retrieve the role type from.
     * @return a role type indicating the current level of authorization.
     */
    public Future<RoleType> getRoleByRequest(Request request) {
        return verifyToken(request.token());
    }

    /**
     * Verifies the given token and returns the roles the token provides authorization for.
     *
     * @param token a user supplied token to verify.
     * @return optionally a set of roles the user belongs to.
     */
    public Future<RoleType> verifyToken(Token token) {
        Future<RoleType> future = Future.future();

        factory.verify(token).setHandler(done -> {
            if (done.succeeded()) {
                future.complete(RoleMap.get(token.getProperty(ID_ROLE).toString()));
            } else {
                future.complete(RoleMap.get(RoleMap.PUBLIC));
            }
        });
        return future;
    }

    /**
     * Verifies the password of the user identified by the given username.
     *
     * @param user     an existing user.
     * @param password the plaintext password of the given username.
     * @return true if the password was verified and
     */
    public Future<Void> verifyPassword(User user, String password) {
        Future<Void> future = Future.future();
        hasher.verify(future, user.getPassword(), password);
        return future;
    }

    /**
     * Retrieves an user by its user name.
     *
     * @param name the name of the user to retrieve.
     * @return an user if one matches the given username, empty otherwise.
     */
    public Optional<User> getUserByName(String name) {
        return ZapperConfig.get().getUsers().stream()
                .filter(user -> user.getUsername().equals(name))
                .findFirst();
    }

    /**
     * Adds a new user to the configuration.
     *
     * @param user the user to add to the configuration.
     */
    public static void addUserToConfiguration(User user) {
        ZapperConfig config = ZapperConfig.get();
        config.getUsers().remove(user);
        config.getUsers().add(user);
        config.save();
    }
}
