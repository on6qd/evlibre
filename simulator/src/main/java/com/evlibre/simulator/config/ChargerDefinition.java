package com.evlibre.simulator.config;

import com.evlibre.simulator.charger.Behavior;

import java.util.ArrayList;
import java.util.List;

public record ChargerDefinition(
        String id,
        String tenant,
        String vendor,
        String model,
        String protocol,
        int evseCount,
        List<String> idTags,
        Behavior behavior
) {

    public static List<ChargerDefinition> expandAll(SimulatorConfig config) {
        List<ChargerDefinition> result = new ArrayList<>();

        // Expand fleets
        for (FleetConfig fleet : config.fleets()) {
            ChargerProfile profile = resolveProfile(config, fleet.profile());
            for (int i = 1; i <= fleet.count(); i++) {
                String id = String.format("%s-%03d", fleet.prefix(), i);
                result.add(new ChargerDefinition(
                        id, fleet.tenant(),
                        profile.vendor(), profile.model(), profile.protocol(),
                        profile.evseCount(), profile.idTags(),
                        fleet.behavior()
                ));
            }
        }

        // Add individual chargers
        for (ChargerConfig charger : config.chargers()) {
            ChargerProfile profile = resolveProfile(config, charger.profile());
            result.add(new ChargerDefinition(
                    charger.id(), charger.tenant(),
                    profile.vendor(), profile.model(), profile.protocol(),
                    profile.evseCount(), profile.idTags(),
                    charger.behavior()
            ));
        }

        return result;
    }

    private static ChargerProfile resolveProfile(SimulatorConfig config, String profileName) {
        if (profileName != null && config.profiles().containsKey(profileName)) {
            return config.profiles().get(profileName);
        }
        return new ChargerProfile(); // defaults
    }
}
