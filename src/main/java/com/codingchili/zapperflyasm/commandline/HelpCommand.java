package com.codingchili.zapperflyasm.commandline;

import io.vertx.core.Future;

import com.codingchili.core.context.Command;
import com.codingchili.core.context.CommandExecutor;
import com.codingchili.core.logging.ConsoleLogger;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 */
public class HelpCommand implements Command {
    private Logger logger = new ConsoleLogger(HelpCommand.class);

    @Override
    public void execute(Future<Boolean> future, CommandExecutor executor) {
        logger.log("Available commands");
        executor.list().forEach(command -> {
            logger.log(command.toString());
        });
        future.complete(true);
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
