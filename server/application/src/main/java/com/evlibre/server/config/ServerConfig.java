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

        @JsonProperty("jdbc_url")
        private String jdbcUrl = "jdbc:h2:./data/evlibre;MODE=PostgreSQL";

        @JsonProperty("username")
        private String username = "sa";

        @JsonProperty("password")
        private String password = "";

        @JsonProperty("pool_size")
        private int poolSize = 10;

        @JsonProperty("run_migrations")
        private boolean runMigrations = true;

        public String type() { return type; }
        public String jdbcUrl() { return jdbcUrl; }
        public String username() { return username; }
        public String password() { return password; }
        public int poolSize() { return poolSize; }
        public boolean runMigrations() { return runMigrations; }
    }
}
