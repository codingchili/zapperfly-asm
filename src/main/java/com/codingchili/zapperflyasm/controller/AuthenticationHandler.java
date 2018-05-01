package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.User;

import java.util.Optional;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.protocol.RoleMap.PUBLIC;

/**
 * @author Robin Duda
 * <p>
 * Handles user logins.
 */
@Roles(PUBLIC)
@Address("authentication")
public class AuthenticationHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private Authenticator authenticator;

    @Override
    public void init(CoreContext core) {
        ZapperContext core1 = ZapperContext.ensure(core);
        this.authenticator = core1.authenticator();
    }

    @Api
    public void login(Request request) {
        String username = request.data().getString("username");
        String password = request.data().getString("password");

        Optional<User> user = authenticator.getUserByName(username);

        if (user.isPresent()) {
            authenticator.verifyPassword(user.get(), password).setHandler(done -> {
                if (done.succeeded()) {
                    request.write(authenticator.generateToken(user.get()));
                } else {
                    request.error(new CoreRuntimeException("The passwords does not match."));
                }
            });
        } else {
            request.error(new CoreRuntimeException("The given username does not exist."));
        }
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(request);
    }
}
