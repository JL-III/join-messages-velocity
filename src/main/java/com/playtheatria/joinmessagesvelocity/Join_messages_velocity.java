package com.playtheatria.joinmessagesvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private static YamlDocument config;
    private NamedTextColor bracketColor;

    @Inject
    public Join_messages_velocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.notifyDisconnectList = new CopyOnWriteArrayList<>();

        try  {
            config = YamlDocument.create(new File(dataDirectory.toFile(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());
            config.update();
            config.save();
            String bracketColorAsString = config.getString(Route.from("bracket-color"));
            bracketColor = Color.fromValue(bracketColorAsString);
        } catch (IOException | IllegalArgumentException ex) {
            logger.error("Could not create/load plugin config, shutting plugin down!");
            logger.error(ex.getMessage());
            Optional<PluginContainer> container = server.getPluginManager().getPlugin("join-messages-velocity");
            container.ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
        }

        logger.info("join-messages-velocity has initialized");
    }

    @Subscribe
    public void onPostLoginEvent(PostLoginEvent event) {
        if (event.getPlayer().hasPermission("join-messages-velocity.login.silent")) return;
        for (Player playerToNotify : server.getAllPlayers()) {
            if (playerToNotify.hasPermission("join-messages-velocity.login.ignore")) continue;
            server.getScheduler()
                    .buildTask(this, () -> {
                        // abort immediately since the player is no longer online
                        if (!event.getPlayer().isActive()) return;
                        sendPlayerMessage(
                                playerToNotify,
                                event.getPlayer().getUsername(),
                                MessageType.CONNECT,
                                bracketColor,
                                NamedTextColor.GREEN,
                                NamedTextColor.GRAY
                        );
                        // check if the player has silenced logout messages, if so, there's no need to add them to the disconnect list
                        if (event.getPlayer().hasPermission("join-messages-velocity.logout.silent")) return;
                        notifyDisconnectList.add(event.getPlayer());
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
                    bracketColor,
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
