package com.codingchili.zapperflyasm.controller;

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
        this.hasher = core.hasher();
        this.factory = core.tokens(ZapperConfig.get().getTokenSecret().getBytes());
    }

    /**
     * @return a token factory used to generate and verify tokens.
     */
    public Token generateToken(User user) {
        Token token = new Token()
                .setDomain(user.getUsername())
                .addProperty(ID_ROLE, user.getRole());
        factory.hmac(token);
        return token;
    }

    /**
     * get the role of the token contained in the given request.
     *
     * @param request carries the token to retrieve the role type from.
     * @return a role type indicating the current level of authorization.
     */
    public RoleType getRoleByRequest(Request request) {
        return verifyToken(request.token());
    }

    /**
     * Verifies the given token and returns the roles the token provides authorization for.
     *
     * @param token a user supplied token to verify.
     * @return optionally a set of roles the user belongs to.
     */
    public RoleType verifyToken(Token token) {
        if (factory.verify(token)) {
            return RoleMap.get(token.getProperty(ID_ROLE).toString());
        } else {
            return RoleMap.get(RoleMap.PUBLIC);
        }
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
