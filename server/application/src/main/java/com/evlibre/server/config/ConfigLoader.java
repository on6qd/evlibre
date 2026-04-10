package com.evlibre.server.config;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    public static ServerConfig load(String[] args) {
        String configPath = "server.toml";

        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) {
                configPath = args[i + 1];
            }
        }

        File configFile = new File(configPath);
        if (configFile.exists()) {
            try {
                TomlMapper mapper = new TomlMapper();
                ServerConfig config = mapper.readValue(configFile, ServerConfig.class);
                log.info("Loaded configuration from {}", configPath);
                return config;
            } catch (IOException e) {
                log.warn("Failed to load {}: {}. Using defaults.", configPath, e.getMessage());
            }
        } else {
            log.info("No config file found at {}. Using defaults.", configPath);
        }

        return new ServerConfig();
    }
}
