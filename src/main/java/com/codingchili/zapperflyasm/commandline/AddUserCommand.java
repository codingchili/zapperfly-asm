package com.codingchili.zapperflyasm.commandline;

import com.codingchili.zapperflyasm.handler.Authenticator;
import com.codingchili.zapperflyasm.model.User;
import io.vertx.core.Future;

import com.codingchili.core.context.*;
import com.codingchili.core.logging.ConsoleLogger;
import com.codingchili.core.protocol.RoleMap;
import com.codingchili.core.security.HashFactory;
import com.codingchili.core.security.SecretFactory;

/**
 * @author Robin Duda
 * <p>
 * A command to add new users.
 */
public class AddUserCommand implements Command {
    private static final String NAME = "--name";
    private static final String PASS = "--pass";
    private static final String ROLE = "--role";
    private final ConsoleLogger logger = new ConsoleLogger(getClass());

    @Override
    public void execute(Future<CommandResult> future, CommandExecutor executor) {
        if (executor.hasProperty(NAME)) {
            CoreContext context = new SystemContext();

            User user = new User()
                    .setUsername(executor.getProperty(NAME).orElse(""))
                    .setRole(executor.getProperty(ROLE).orElseGet(() -> {
                        String role = RoleMap.USER;
                        logger.log("using default role '" + role + "'.");
                        return role;
                    }));

            new HashFactory(context).hash(
                    executor.getProperty(PASS).orElseGet(() -> {
                        String password = SecretFactory.generate(24);
                        logger.log(String.format("generated password '%s'.", password));
                        return password;
                    })).setHandler(done -> {
                Authenticator.addUserToConfiguration(user.setPassword(done.result()));
                logger.log("created user '" + user.getUsername() + "'.");
                future.complete(CommandResult.SHUTDOWN);
                context.close();
            });
        } else {
            logger.log("--name <userName> is required.");
            future.complete(CommandResult.SHUTDOWN);
        }
    }

    @Override
    public String getDescription() {
        return "--user adds a new user with options" +
                "\n\t\t--user <userName>" +
                "\n\t\t--pass <passWord>" +
                "\n\t\t--role <admin|user>";
    }

    @Override
    public String getName() {
        return "--user";
    }
}
