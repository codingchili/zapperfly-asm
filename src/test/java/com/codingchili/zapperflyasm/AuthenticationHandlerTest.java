package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.AuthenticationHandler;
import com.codingchili.zapperflyasm.controller.Authenticator;
import com.codingchili.zapperflyasm.model.User;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.protocol.ResponseStatus;
import com.codingchili.core.security.HashFactory;
import com.codingchili.core.testing.RequestMock;

import static com.codingchili.core.configuration.CoreStrings.*;

/**
 * @author Robin Duda
 * <p>
 * Tests for the authentication handler.
 */
@RunWith(VertxUnitRunner.class)
public class AuthenticationHandlerTest {
    private static final String ROUTE_LOGIN = "login";
    private static AuthenticationHandler handler;
    private static CoreContext core;

    @BeforeClass
    public static void setUp() {
        core = new ZapperContextMock();
        handler = new AuthenticationHandler();
        handler.init(core);
    }

    @AfterClass
    public static void tearDown() {
        core.close();
    }

    @Test
    public void tryAuthenticate(TestContext test) {
        Async async = test.async();
        String username = "user";
        String password = "password";
        Authenticator.addUserToConfiguration(new User()
                .setUsername(username)
                .setPassword(new HashFactory(null).hash(password)));

        handler.handle(RequestMock.get(ROUTE_LOGIN, ((response, status) -> {
            test.assertEquals(ResponseStatus.ACCEPTED, status);
            test.assertTrue(response.containsKey(ID_DOMAIN));
            test.assertTrue(response.containsKey(ID_KEY));
            async.complete();
        }), new JsonObject()
                .put(ID_USERNAME, username)
                .put(ID_PASSWORD, password)));
    }

    @Test
    public void hashFact(TestContext test) {
        Async async = test.async();
        HashFactory hash = new HashFactory(core);
        String argon =  hash.hash("pass");
        hash.verify(done -> {
            System.out.println(done.succeeded());
            async.complete();
        }, argon, "pass");
    }
}
