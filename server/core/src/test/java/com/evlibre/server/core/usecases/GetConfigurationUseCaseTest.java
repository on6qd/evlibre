package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.StationConfigurationKey;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationConfigurationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class GetConfigurationUseCaseTest {

    private StubCommandSender commandSender;
    private FakeConfigPort configPort;
    private GetConfigurationUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        configPort = new FakeConfigPort();
        useCase = new GetConfigurationUseCase(commandSender, configPort);
    }

    @Test
    void parses_and_stores_configuration_keys() {
        commandSender.setNextResponse(Map.of(
                "configurationKey", List.of(
                        Map.of("key", "HeartbeatInterval", "value", "300", "readonly", false),
                        Map.of("key", "MeterValuesSampledData", "value", "Energy.Active.Import.Register", "readonly", true)
                )
        ));

        List<StationConfigurationKey> result = useCase.getConfiguration(tenantId, station, List.of()).join();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).key()).isEqualTo("HeartbeatInterval");
        assertThat(result.get(0).value()).isEqualTo("300");
        assertThat(result.get(0).readonly()).isFalse();
        assertThat(result.get(1).readonly()).isTrue();
        assertThat(configPort.saved).hasSize(2);
    }

    @Test
    void empty_response_returns_empty_list() {
        commandSender.setNextResponse(Map.of());

        List<StationConfigurationKey> result = useCase.getConfiguration(tenantId, station, null).join();

        assertThat(result).isEmpty();
    }

    @Test
    void specific_keys_are_included_in_payload() {
        commandSender.setNextResponse(Map.of("configurationKey", List.of(
                Map.of("key", "HeartbeatInterval", "value", "300", "readonly", false)
        )));

        useCase.getConfiguration(tenantId, station, List.of("HeartbeatInterval")).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetConfiguration");
        assertThat(cmd.payload()).containsKey("key");
    }

    static class FakeConfigPort implements StationConfigurationPort {
        final List<StationConfigurationKey> saved = new ArrayList<>();

        @Override
        public void saveConfiguration(TenantId tenantId, ChargePointIdentity stationIdentity,
                                       List<StationConfigurationKey> keys) {
            saved.clear();
            saved.addAll(keys);
        }

        @Override
        public void updateConfigurationKey(TenantId tenantId, ChargePointIdentity stationIdentity,
                                            StationConfigurationKey key) {
            for (int i = 0; i < saved.size(); i++) {
                if (saved.get(i).key().equalsIgnoreCase(key.key())) {
                    saved.set(i, new StationConfigurationKey(
                            saved.get(i).key(), key.value(), saved.get(i).readonly()));
                    return;
                }
            }
            saved.add(key);
        }

        @Override
        public Optional<List<StationConfigurationKey>> getConfiguration(TenantId tenantId,
                                                                         ChargePointIdentity stationIdentity) {
            return saved.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(saved));
        }
    }
}
