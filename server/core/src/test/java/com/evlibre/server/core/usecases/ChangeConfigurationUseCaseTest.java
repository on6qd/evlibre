package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationConfigurationPort;
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
            savedKeys.addAll(keys);
        }

        @Override
        public Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                         ChargePointIdentity stationIdentity) {
            return savedKeys.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(savedKeys));
        }
    }
}
