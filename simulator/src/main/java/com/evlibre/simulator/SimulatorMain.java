package com.evlibre.simulator;

import com.evlibre.simulator.charger.SimulatedCharger;
import com.evlibre.simulator.config.ChargerDefinition;
import com.evlibre.simulator.config.SimulatorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SimulatorMain {

    private static final Logger log = LoggerFactory.getLogger(SimulatorMain.class);

    public static void main(String[] args) throws Exception {
        String configPath = resolveConfigPath(args);
        log.info("Loading config from: {}", configPath);

        SimulatorConfig config = SimulatorConfig.load(configPath);
        List<ChargerDefinition> definitions = ChargerDefinition.expandAll(config);

        if (definitions.isEmpty()) {
            log.error("No chargers defined in config. Add [[fleets]] or [[chargers]] sections.");
            System.exit(1);
        }

        log.info("Starting simulator with {} charger(s)", definitions.size());
        log.info("  Server: {}", config.defaults().serverUrl());

        // Group by tenant for logging
        definitions.stream()
                .collect(java.util.stream.Collectors.groupingBy(ChargerDefinition::tenant,
                        java.util.stream.Collectors.counting()))
                .forEach((tenant, count) -> log.info("  Tenant '{}': {} charger(s)", tenant, count));

        Vertx vertx = Vertx.vertx();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        List<SimulatedCharger> chargers = new ArrayList<>();
        for (ChargerDefinition def : definitions) {
            chargers.add(new SimulatedCharger(vertx, def, config.defaults(), objectMapper));
        }

        // Connect with staggered delays to avoid thundering herd
        long staggerMs = Math.max(50, 5000 / Math.max(1, definitions.size()));
        for (int i = 0; i < chargers.size(); i++) {
            SimulatedCharger charger = chargers.get(i);
            long delay = 1 + (long) i * staggerMs;
            vertx.setTimer(delay, id -> charger.connect());
        }

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down simulator...");
            for (SimulatedCharger charger : chargers) {
                charger.disconnect();
            }
            vertx.close();
            log.info("Simulator stopped.");
        }));
    }

    private static String resolveConfigPath(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) {
                return args[i + 1];
            }
        }
        // Default: look for simulator.toml in current directory, then classpath
        java.io.File local = new java.io.File("simulator.toml");
        if (local.exists()) {
            return local.getAbsolutePath();
        }
        // Try the module's resources directory
        java.io.File moduleResource = new java.io.File("simulator/src/main/resources/simulator.toml");
        if (moduleResource.exists()) {
            return moduleResource.getAbsolutePath();
        }
        log.error("No config file found. Use --config <path> or place simulator.toml in the current directory.");
        System.exit(1);
        return null;
    }
}
