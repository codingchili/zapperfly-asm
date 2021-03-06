package com.codingchili.zapperflyasm.commandline;

import com.codingchili.core.context.Command;
import com.codingchili.core.context.CommandExecutor;
import com.codingchili.core.context.CommandResult;
import com.codingchili.core.logging.ConsoleLogger;
import com.codingchili.core.logging.Logger;

import io.vertx.core.Future;

/**
 * @author Robin Duda
 * <p>
 * A command to list all available commands.
 */
public class HelpCommand implements Command {
    private Logger logger = new ConsoleLogger(HelpCommand.class);

    @Override
    public void execute(Future<CommandResult> future, CommandExecutor executor) {
        logger.log("Available commands\n");
        executor.list().forEach(command -> {
            logger.log(command.getDescription() + "\n");
        });
        future.complete(CommandResult.SHUTDOWN);
    }

    @Override
    public String getDescription() {
        return "--help lists available commands.";
    }

    @Override
    public String getName() {
        return "--help";
    }
}
