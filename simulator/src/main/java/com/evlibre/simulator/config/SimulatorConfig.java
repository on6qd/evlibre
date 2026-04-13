package com.evlibre.simulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SimulatorConfig {

    public DefaultsConfig defaults = new DefaultsConfig();
    public Map<String, ChargerProfile> profiles = Map.of();
    public List<FleetConfig> fleets = List.of();
    public List<ChargerConfig> chargers = List.of();

    public DefaultsConfig defaults() { return defaults; }
    public Map<String, ChargerProfile> profiles() { return profiles; }
    public List<FleetConfig> fleets() { return fleets; }
    public List<ChargerConfig> chargers() { return chargers; }

    public static SimulatorConfig load(String path) throws IOException {
        ObjectMapper toml = new TomlMapper();
        return toml.readValue(new File(path), SimulatorConfig.class);
    }
}
