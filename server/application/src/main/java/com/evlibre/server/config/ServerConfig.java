package com.evlibre.server.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig {

    @JsonProperty("ocpp")
    private OcppConfig ocpp = new OcppConfig();

    @JsonProperty("database")
    private DatabaseConfig database = new DatabaseConfig();

    public OcppConfig ocpp() { return ocpp; }
    public DatabaseConfig database() { return database; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OcppConfig {
        @JsonProperty("websocket_port")
        private int websocketPort = 9090;

        @JsonProperty("heartbeat_interval")
        private int heartbeatInterval = 900;

        public int websocketPort() { return websocketPort; }
        public int heartbeatInterval() { return heartbeatInterval; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DatabaseConfig {
        @JsonProperty("type")
        private String type = "in-memory";

        public String type() { return type; }
    }
}
