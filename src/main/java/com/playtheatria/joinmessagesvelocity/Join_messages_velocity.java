package com.playtheatria.joinmessagesvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

@Plugin(
        id = "join-messages-velocity",
        name = "join-messages-velocity",
        version = BuildConstants.VERSION
)
public class Join_messages_velocity {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public Join_messages_velocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent event) {
        if (event.getPlayer().hasPermission("join-messages-velocity.login.silent")) return;
        logger.info("{} has joined and was detected by join-messages-velocity", event.getPlayer().getUsername());
        for (Player player : server.getAllPlayers()) {
            if (!player.hasPermission("join-messages-velocity.login.ignore")) {
                player.sendMessage(
                        Component.text("[", NamedTextColor.DARK_GRAY).append(
                        Component.text("+", NamedTextColor.GREEN).append(
                        Component.text("] ", NamedTextColor.DARK_GRAY).append(
                        Component.text(event.getPlayer().getUsername(), NamedTextColor.DARK_GRAY))))
                );
            }
        }
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent event) {
        if (event.getPlayer().hasPermission("join-messages-velocity.logout.silent")) return;
        logger.info("{} has disconnected and was detected by join-messages-velocity", event.getPlayer().getUsername());
        for (Player player : server.getAllPlayers()) {
            if (!player.hasPermission("join-messages-velocity.logout.ignore")) {
                player.sendMessage(
                        Component.text("[", NamedTextColor.DARK_GRAY).append(
                                Component.text("-", NamedTextColor.DARK_AQUA).append(
                                        Component.text("] ", NamedTextColor.DARK_GRAY).append(
                                                Component.text(event.getPlayer().getUsername(), NamedTextColor.DARK_GRAY))))
                );            }
        }
    }
}
