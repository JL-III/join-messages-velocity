package com.playtheatria.joinvelocity.commands;

import com.playtheatria.joinvelocity.config.Config;
import com.playtheatria.joinvelocity.discord.Discord;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

public class JoinVelocityCommand implements SimpleCommand {
    private final Config config;
    private final Discord discord;

    public JoinVelocityCommand(Config config, Discord discord) {
        this.config = config;
        this.discord = discord;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source.hasPermission(config.getAdminPermission()) && args.length == 1) {
            switch (args[0]) {
                case "reload" -> {
                    config.reloadConfig();
                    discord.configReloaded();
                }
                default -> {}
            }
        }
    }
}
