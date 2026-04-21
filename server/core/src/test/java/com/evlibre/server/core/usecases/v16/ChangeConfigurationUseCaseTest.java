package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.v16.model.StationConfigurationKey;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.outbound.StationConfigurationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeConfigurationUseCaseTest {

    private StubCommandSender commandSender;
    private FakeConfigurationPort configPort;
    private ChangeConfigurationUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        configPort = new FakeConfigurationPort();
        useCase = new ChangeConfigurationUseCase(commandSender, configPort);
    }

    @Test
    void accepted_change_updates_config_store() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.changeConfiguration(tenantId, station, "HeartbeatInterval", "300").join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(configPort.savedKeys).hasSize(1);
        assertThat(configPort.savedKeys.get(0).key()).isEqualTo("HeartbeatInterval");
        assertThat(configPort.savedKeys.get(0).value()).isEqualTo("300");
    }

    // OCPP 1.6: configuration keys are case-insensitive. Updating "heartbeatinterval"
    // must modify the same entry as an existing "HeartbeatInterval" — not create a duplicate.
    @Test
    void accepted_change_is_case_insensitive() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.changeConfiguration(tenantId, station, "HeartbeatInterval", "300").join();
        useCase.changeConfiguration(tenantId, station, "heartbeatinterval", "600").join();

        assertThat(configPort.savedKeys).hasSize(1);
        assertThat(configPort.savedKeys.get(0).key()).isEqualTo("HeartbeatInterval"); // original casing preserved
        assertThat(configPort.savedKeys.get(0).value()).isEqualTo("600"); // value updated
    }

    @Test
    void rejected_change_does_not_update_config() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.changeConfiguration(tenantId, station, "ReadOnlyKey", "value").join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(configPort.savedKeys).isEmpty();
    }

    static class FakeConfigurationPort implements StationConfigurationPort {
        final List<StationConfigurationKey> savedKeys = new ArrayList<>();

        @Override
        public void saveConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                                       List<StationConfigurationKey> keys) {
            savedKeys.clear();
            savedKeys.addAll(keys);
        }

        @Override
        public void updateConfigurationKey(TenantId tenantId, ChargePointIdentity stationIdentity,
                                            StationConfigurationKey key) {
            for (int i = 0; i < savedKeys.size(); i++) {
                if (savedKeys.get(i).key().equalsIgnoreCase(key.key())) {
                    savedKeys.set(i, new StationConfigurationKey(
                            savedKeys.get(i).key(), key.value(), savedKeys.get(i).readonly()));
                    return;
                }
            }
            savedKeys.add(key);
        }

        @Override
        public Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                         ChargePointIdentity stationIdentity) {
            return savedKeys.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(savedKeys));
        }
    }
}
