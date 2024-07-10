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
    private final List<Player> notifyDisconnectList;

    @Inject
    public Join_messages_velocity(ProxyServer server) {
        this.server = server;
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
                        sendPlayerMessage(
                                player,
                                event.getPlayer().getUsername(),
                                MessageType.CONNECT,
                                NamedTextColor.DARK_GRAY,
                                NamedTextColor.GREEN,
                                NamedTextColor.GRAY
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
            sendPlayerMessage(
                    player,
                    event.getPlayer().getUsername(),
                    MessageType.DISCONNECT,
                    NamedTextColor.DARK_GRAY,
                    NamedTextColor.DARK_AQUA,
                    NamedTextColor.GRAY
            );
        }
        notifyDisconnectList.remove(event.getPlayer());
    }

    private void sendPlayerMessage(
            Player targetPlayer,
            String userName,
            MessageType messageType,
            NamedTextColor bracketColor,
            NamedTextColor symbolColor,
            NamedTextColor userNameColor
    ) {
        targetPlayer.sendMessage(
                Component.text("[", bracketColor).append(
                        Component.text(messageType.getSymbol(), symbolColor).append(
                                Component.text("] ", bracketColor).append(
                                        Component.text(userName, userNameColor))))
        );
    }
}
