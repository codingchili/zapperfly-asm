package com.codingchili.zapperflyasm.handler;

import com.codingchili.zapperflyasm.model.User;
import com.codingchili.zapperflyasm.model.ZapperConfig;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;
import com.codingchili.core.protocol.RoleMap;
import com.codingchili.core.security.HashFactory;

/**
 * @author Robin Duda
 *
 * Tests for client authentication, tokens passwords, adding new users.
 */
@RunWith(VertxUnitRunner.class)
public class AuthenticatorTest {
    private static final String adminRole = "admin";
    private static CoreContext core;
    private static Authenticator authenticator;

    @BeforeClass
    public static void setUp() {
        core = new SystemContext();
        authenticator = new Authenticator(core);
    }

    @AfterClass
    public static void tearDown() {
        core.close();
    }

    @Test
    public void addUserToConfiguration(TestContext test) {
        String name = "test1";

        ZapperConfig.get().getUsers().add(new User()
                .setUsername(name));

        test.assertTrue(authenticator.getUserByName(name).isPresent());
        test.assertFalse(authenticator.getUserByName("missing").isPresent());
    }

    @Test
    public void verifyPassword(TestContext test) {
        Async async = test.async();
        String plaintext = "pass";

        new HashFactory(core).hash(plaintext).setHandler(password -> {
            User user = new User()
                    .setUsername("test")
                    .setPassword(password.result());

            Authenticator.addUserToConfiguration(user);

            authenticator.verifyPassword(user, plaintext).setHandler(done -> {
                test.assertTrue(done.succeeded());

                authenticator.verifyPassword(user, "another").setHandler(done2 -> {
                    test.assertFalse(done2.succeeded());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void generateToken(TestContext test) {
        Async async = test.async();
        User user = new User()
                .setUsername("theUser")
                .setRole(adminRole);

        authenticator.generateToken(user).setHandler(token -> {
            authenticator.verifyToken(token.result()).setHandler(role -> {
                test.assertEquals(role.result().getName(), adminRole);

                authenticator.verifyToken(token.result().setDomain("root")).setHandler(fakeRole -> {
                    test.assertEquals(fakeRole.result().getName(), RoleMap.PUBLIC);
                    async.complete();
                });
            });
        });
    }
}
