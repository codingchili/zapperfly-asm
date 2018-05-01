package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.Authenticator;
import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.User;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;
import com.codingchili.core.protocol.RoleMap;
import com.codingchili.core.security.HashFactory;
import com.codingchili.core.security.Token;

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
        String password = "pass";
        User user = new User()
                .setUsername("test")
                .setPassword(new HashFactory(null).hash(password));

        Authenticator.addUserToConfiguration(user);

        authenticator.verifyPassword(user, password).setHandler(done -> {
            test.assertTrue(done.succeeded());

            authenticator.verifyPassword(user, "another").setHandler(done2 -> {
                test.assertFalse(done2.succeeded());
                async.complete();
            });
        });
    }

    @Test
    public void generateToken(TestContext test) {
        User user = new User()
                .setUsername("theUser")
                .setRole(adminRole);

        Token token = authenticator.generateToken(user);
        test.assertEquals(authenticator.verifyToken(token).getName(), adminRole);
        test.assertEquals(authenticator.verifyToken(token.setDomain("root")).getName(), RoleMap.PUBLIC);
    }
}
