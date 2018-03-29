package com.codingchili.zapperflyasm.commandline;

import io.vertx.core.Future;

import com.codingchili.core.context.*;
import com.codingchili.core.logging.ConsoleLogger;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * A command to list all available commands.
 */
public class HelpCommand implements Command {
    private Logger logger = new ConsoleLogger(HelpCommand.class);

    @Override
    public void execute(Future<CommandResult> future, CommandExecutor executor) {
        logger.log("Available commands");
        executor.list().forEach(command -> {
            logger.log(command.toString());
        });
        future.complete(CommandResult.SHUTDOWN);
    }

    @Override
    public String getDescription() {
        return "lists available commands.";
    }

    @Override
    public String getName() {
        return "--help";
    }

    @Override
    public String toString() {
        return getName() + " " + getDescription();
    }
}
