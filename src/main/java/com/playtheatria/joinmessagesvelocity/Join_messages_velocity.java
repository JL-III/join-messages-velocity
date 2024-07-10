package com.playtheatria.joinmessagesvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "join-messages-velocity",
        name = "join-messages-velocity",
        version = BuildConstants.VERSION
)
public class Join_messages_velocity {

    private final ProxyServer server;
    private final Logger logger;
    private final List<Player> notifyDisconnectList;

    @Inject
    public Join_messages_velocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.notifyDisconnectList = new CopyOnWriteArrayList<>();
    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent event) {
        if (event.getPlayer().hasPermission("join-messages-velocity.login.silent")) return;
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("join-messages-velocity.login.ignore")) continue;
            server.getScheduler()
                    .buildTask(this, () -> {
                        // abort immediately since the player is no longer online
                        if (!player.isActive()) return;
                        player.sendMessage(
                                Component.text("[", NamedTextColor.DARK_GRAY).append(
                                        Component.text("+", NamedTextColor.GREEN).append(
                                                Component.text("] ", NamedTextColor.DARK_GRAY).append(
                                                        Component.text(event.getPlayer().getUsername(), NamedTextColor.DARK_GRAY))))
                        );
                        // check if the player has silenced logout messages, if so, there's no need to add them to the disconnect list
                        if (player.hasPermission("join-messages-velocity.logout.silent")) return;
                        notifyDisconnectList.add(player);
                    })
                    .delay(3L, TimeUnit.SECONDS) // this time is pretty arbitrary, we just want to give enough time for other processes to do their thing
                    .schedule();
        }
    }

    @Subscribe
    public void onDisconnectEvent(DisconnectEvent event) {
        // we check if the player is in the disconnect list, if not we abort immediately
        if (!notifyDisconnectList.contains(event.getPlayer())) return;

        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("join-messages-velocity.logout.ignore")) continue;
            player.sendMessage(
                    Component.text("[", NamedTextColor.DARK_GRAY).append(
                            Component.text("-", NamedTextColor.DARK_AQUA).append(
                                    Component.text("] ", NamedTextColor.DARK_GRAY).append(
                                            Component.text(event.getPlayer().getUsername(), NamedTextColor.DARK_GRAY))))
            );
        }
        notifyDisconnectList.remove(event.getPlayer());
    }
}
