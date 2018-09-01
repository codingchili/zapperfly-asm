package com.codingchili.zapperflyasm.commandline;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import io.vertx.core.Future;

import com.codingchili.core.context.*;
import com.codingchili.core.logging.ConsoleLogger;

/**
 * @author Robin Duda
 *
 * A command used to dump the default configuration to file.
 */
public class ConfigureCommand implements Command {
    @Override
    public void execute(Future<CommandResult> future, CommandExecutor executor) {
        ZapperConfig.get().save();
        new ConsoleLogger().log("Configuration saved to " + ZapperConfig.get().getPath());
        future.complete(CommandResult.SHUTDOWN);
    }

    @Override
    public String getDescription() {
        return "Generates a zapperfly.yaml configuration file if one is missing.";
    }

    @Override
    public String getName() {
        return "--configure";
    }
}
