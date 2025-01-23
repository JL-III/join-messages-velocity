package com.playtheatria.joinvelocity;

import com.google.inject.Inject;
import com.playtheatria.joinvelocity.commands.JoinVelocityCommand;
import com.playtheatria.joinvelocity.config.Config;
import com.playtheatria.joinvelocity.discord.Discord;
import com.playtheatria.joinvelocity.enums.MessageType;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "join-velocity",
        name = "join-velocity",
        version = BuildConstants.VERSION
)
public class JoinVelocity {
    private final ProxyServer server;
    private final Config config;
    private final Logger logger;
    private final Discord discord;
    private final List<Player> notifyDisconnectList;

    @Inject
    public JoinVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.config = new Config(dataDirectory.resolve("config.yml"), logger);
        this.discord = new Discord(config, logger);
        this.notifyDisconnectList = new CopyOnWriteArrayList<>();
        CommandMeta meta = server.getCommandManager().metaBuilder("joinvelocity")
                        .aliases("jv")
                        .plugin(this)
                        .build();
        server.getCommandManager().register(meta, new JoinVelocityCommand(config, discord));
        logger.info("join-velocity has initialized");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        discord.onProxyInitialize();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        discord.onProxyShutdown();
    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent event) {
        if (event.getPlayer().hasPermission(config.getSilentServerLoginPermission())) {
            logger.info(String.format("Player: %s had %s skipping login message!", event.getPlayer().getUsername(), config.getSilentServerLoginPermission()));
            return;
        }
        for (Player playerToNotify : server.getAllPlayers()) {
            if (!playerToNotify.hasPermission(config.getLoginIgnorePermission())) {
                server.getScheduler()
                        .buildTask(this, () -> {
                            // abort immediately since the player is no longer online
                            if (!event.getPlayer().isActive()) return;
                            sendPlayerMessage(
                                    playerToNotify,
                                    event.getPlayer().getUsername(),
                                    MessageType.CONNECT,
                                    config.getBracketColor(),
                                    NamedTextColor.GREEN
                            );
                            // check if the player has silenced logout messages, if so, there's no need to add them to the disconnect list
                            if (!event.getPlayer().hasPermission(config.getSilentServerLogoutPermission())) {
                                notifyDisconnectList.add(event.getPlayer());
                            }
                        })
                        .delay(3L, TimeUnit.SECONDS) // this time is pretty arbitrary, we just want to give enough time for other processes to do their thing
                        .schedule();
            }
        }
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent event) {
        // we check if the player is in the disconnect list, if not we abort immediately
        if (!notifyDisconnectList.contains(event.getPlayer())) return;

        if (event.getPlayer().hasPermission(config.getSilentServerLogoutPermission())) {
            logger.info(String.format("Player: %s had %s skipping login message!", event.getPlayer().getUsername(), config.getSilentServerLogoutPermission()));
            return;
        }

        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission(config.getLogoutIgnorePermission())) { continue; }
            sendPlayerMessage(
                    player,
                    event.getPlayer().getUsername(),
                    MessageType.DISCONNECT,
                    config.getBracketColor(),
                    NamedTextColor.DARK_AQUA
            );
        }
        notifyDisconnectList.remove(event.getPlayer());
    }

    @Subscribe
    public void onConnectProcessDiscord(ServerConnectedEvent event) {
        if (event.getPlayer().hasPermission(config.getSilentDiscordLoginPermission())) return;
        String server = event.getServer().getServerInfo().getName();

        String username = event.getPlayer().getUsername();
        Optional<RegisteredServer> previousServer = event.getPreviousServer();
        String previousName = previousServer.map(s -> s.getServerInfo().getName()).orElse(null);

        // if previousServer is disabled but the current server is not, treat it as a join
        if (previousServer.isPresent()) {
            discord.onServerSwitch(username, server, previousName);
        } else {
            discord.onJoin(username, server);
        }
    }

    @Subscribe
    public void onDisconnectProcessDiscord(DisconnectEvent event) {
        if (event.getPlayer().hasPermission(config.getSilentDiscordLogoutPermission())) return;
        Optional<ServerConnection> serverConnection = event.getPlayer().getCurrentServer();
        String username = event.getPlayer().getUsername();

        if (serverConnection.isEmpty()) {
            discord.onDisconnect(username);
        } else {
            discord.onDisconnect(username, serverConnection.get().getServerInfo().getName());
        }
    }

    private void sendPlayerMessage(Player targetPlayer, String userName, MessageType messageType, NamedTextColor bracketColor, NamedTextColor symbolColor) {
        targetPlayer.sendMessage(
                Component.text("[", bracketColor).append(
                        Component.text(messageType.getSymbol(), symbolColor).append(
                                Component.text("] ", bracketColor).append(
                                        Component.text(userName, NamedTextColor.GRAY))))
        );
    }
}
