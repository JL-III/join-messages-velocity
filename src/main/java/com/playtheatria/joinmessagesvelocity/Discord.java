package com.playtheatria.joinmessagesvelocity;

import com.playtheatria.joinmessagesvelocity.config.Config;
import com.playtheatria.joinmessagesvelocity.utils.StringTemplate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.*;

public class Discord extends ListenerAdapter {
    private final Config config;
    private final Logger logger;
    private JDA jda;

    private TextChannel activeChannel;

    public boolean ready = false;

    // queue of Object because multiple types of messages and
    // cant create a common RestAction object without activeChannel
    private final Queue<Object> preReadyQueue = new ArrayDeque<>();

    public Discord(Config config, Logger logger) {
        this.config = config;
        this.logger = logger;
        configReloaded();
    }

    public void configReloaded() {
        String token = config.getToken();

        var builder = JDABuilder.createDefault(token)
                .setChunkingFilter(ChunkingFilter.ALL) //
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(this);
        try {
            jda = builder.build();
            logger.info("jda token: " + jda.getToken());
        } catch (Exception e) {
            this.logger.error("Failed to login to discord: {}", e.getMessage());
        }
    }

    public void shutdown() {
        jda.shutdown();
    }

    // region JDA events

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        logger.info(MessageFormat.format(
                "Bot ready, Guilds: {0} ({1} available)",
                event.getGuildTotalCount(),
                event.getGuildAvailableCount()
        ));

        String channelID = "846790576870522940";
        var channel = jda.getTextChannelById(Objects.requireNonNull(channelID));

        if (channel == null) {
            logger.error("Could not load channel with id: " + channelID);
            return;
        } else {
            logger.info("loaded channel: " + channelID);
        }

        logger.info("Loaded channel: " + channel.getName());

        if (!channel.canTalk()) {
            logger.error("Cannot talk in configured channel");
            return;
        }

        activeChannel = channel;

        var guild = activeChannel.getGuild();

        guild.upsertCommand("list", "list players").queue();

        this.ready = true;

        for (var msg : preReadyQueue) {
            if (msg instanceof String message) {
                activeChannel.sendMessage(message).queue();
            } else if (msg instanceof MessageEmbed embed) {
                activeChannel.sendMessageEmbeds(embed).queue();
            } else {
                logger.warn("Unknown message type in preReadyQueue:{} ", msg);
            }
        }
    }

    public void onJoin(String username, String server) {
        String join_message = "**{username} joined the network**";
        var message = new StringTemplate(join_message)
                .add("username", username)
                .add("server", server)
                .toString();

        sendEmbedMessage(message, Color.GREEN);
    }

    public void onServerSwitch(String username, String current, String previous) {
        String server_switch = "**{username} moved to {current} from {previous}**";
        var message = new StringTemplate(server_switch)
                .add("username", username)
                .add("current", current)
                .add("previous", previous)
                .toString();

        sendEmbedMessage(message, Color.BLUE);
    }

    public void onDisconnect(String username) {
        var message = new StringTemplate("**{username} left the network**")
                .add("username", username)
                .toString();

        sendEmbedMessage(message, Color.RED);
    }

    public void onDisconnect(String username, String server) {
        var message = new StringTemplate("**{username} left the network**")
                .add("username", username)
                .add("server", server)
                .toString();

        sendEmbedMessage(message, Color.RED);
    }

    public void onProxyInitialize() {
        sendEmbedMessage("**Proxy started**", Color.GREEN);
    }

    public void onProxyShutdown() {
        sendEmbedMessage("**Proxy stopped**", Color.RED);
        jda.shutdown();
    }

    // this will be triggered by plugin messages (if possible)
    public void onServerStart(String server) {
        var message = new StringTemplate("**{server} has started**")
                .add("server", server)
                .toString();

        sendEmbedMessage(message, Color.GREEN);
    }

    // this will be triggered by plugin messages (if possible)
    public void onServerStop(String server) {
        var message = new StringTemplate("**{server} has stopped**")
                .add("server", server)
                .toString();

        sendEmbedMessage(message, Color.RED);
    }

    private void sendEmbedMessage(String message, Color color) {
        var embed = new EmbedBuilder().setDescription(message);
        embed.setColor(color);

        if (ready) {
            activeChannel.sendMessageEmbeds(embed.build()).queue();
        } else {
            preReadyQueue.add(embed.build());
        }
    }
}
