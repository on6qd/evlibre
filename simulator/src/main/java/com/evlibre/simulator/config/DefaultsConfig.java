package com.evlibre.simulator.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultsConfig {

    @JsonProperty("server_url")
    public String serverUrl = "ws://localhost:9090";

    @JsonProperty("heartbeat_interval")
    public int heartbeatInterval = 60;

    @JsonProperty("meter_interval")
    public int meterInterval = 15;

    public String serverUrl() { return serverUrl; }
    public int heartbeatInterval() { return heartbeatInterval; }
    public int meterInterval() { return meterInterval; }
}
