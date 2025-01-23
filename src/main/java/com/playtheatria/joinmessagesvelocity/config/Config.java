package com.playtheatria.joinmessagesvelocity.config;

import com.playtheatria.joinmessagesvelocity.enums.Color;
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
    private final String silentLoginPermission = "join-messages-velocity.login.silent";
    private final String silentLogoutPermission = "join-messages-velocity.logout.silent";
    private final String loginIgnorePermission = "join-messages-velocity.login.ignore";
    private final String logoutIgnorePermission = "join-messages-velocity.logout.ignore";

    public Config(Path configPath, Logger logger) {
        this.configPath = configPath;
        this.logger = logger;
        loadConfig();
    }

    public String getToken() {
        return token;
    }

    public NamedTextColor getBracketColor() { return Color.fromValue(bracketColor); }

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

    public String getSilentLoginPermission() {
        return silentLoginPermission;
    }

    public String getSilentLogoutPermission() {
        return silentLogoutPermission;
    }

    public String getLoginIgnorePermission() {
        return loginIgnorePermission;
    }

    public String getLogoutIgnorePermission() {
        return logoutIgnorePermission;
    }
}
