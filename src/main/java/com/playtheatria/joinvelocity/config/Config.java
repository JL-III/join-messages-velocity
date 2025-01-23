package com.playtheatria.joinvelocity.config;

import com.playtheatria.joinvelocity.enums.Color;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private final Path configPath;
    private final Properties properties = new Properties();
    private final Logger logger;
    private String token;
    private String bracketColor;
    private final String adminPermission = "join-velocity.admin";
    private final String whitelistPermission = "join-velocity.whitelist";
    private final String silentServerLoginPermission = "join-velocity.server.login.silent";
    private final String silentServerLogoutPermission = "join-velocity.server.logout.silent";
    private final String silentDiscordLoginPermission = "join-velocity.discord.login.silent";
    private final String silentDiscordLogoutPermission = "join-velocity.discord.logout.silent";
    private final String loginIgnorePermission = "join-velocity.server.login.ignore";
    private final String logoutIgnorePermission = "join-velocity.server.logout.ignore";

    public Config(Path configPath, Logger logger) {
        this.configPath = configPath;
        this.logger = logger;
        loadConfig();
    }

    public String getToken() {
        return token;
    }

    public NamedTextColor getBracketColor() { return Color.fromValue(bracketColor); }

    public void reloadConfig() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            // Ensure the data directory exists
            Files.createDirectories(configPath.getParent());

            // Load default config if it doesn't exist
            if (Files.notExists(configPath)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                        logger.info("Default configuration created at {}", configPath);
                    } else {
                        logger.error("Default configuration file not found in resources.");
                    }
                }
            }

            // Load the configuration
            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
                token = getConfigValue("discord-token");
                bracketColor = getConfigValue("bracket-color");
                logger.info("Configuration loaded successfully.");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    public String getConfigValue(String key) {
        return properties.getProperty(key);
    }

    public String getAdminPermission() {
        return adminPermission;
    }

    public String getWhitelistPermission() { return whitelistPermission; }

    public String getSilentServerLoginPermission() {
        return silentServerLoginPermission;
    }

    public String getSilentServerLogoutPermission() {
        return silentServerLogoutPermission;
    }

    public String getLoginIgnorePermission() {
        return loginIgnorePermission;
    }

    public String getLogoutIgnorePermission() {
        return logoutIgnorePermission;
    }

    public String getSilentDiscordLoginPermission() {
        return silentDiscordLoginPermission;
    }

    public String getSilentDiscordLogoutPermission() {
        return silentDiscordLogoutPermission;
    }
}
