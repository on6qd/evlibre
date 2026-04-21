package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateFirmwareUseCaseTest {

    private StubCommandSender commandSender;
    private UpdateFirmwareUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new UpdateFirmwareUseCase(commandSender);
    }

    @Test
    void sends_location_and_retrieveDate() {
        commandSender.setNextResponse(Map.of());

        useCase.updateFirmware(tenantId, station,
                "ftp://example.com/fw.bin", "2025-06-01T00:00:00Z", 3, 120).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("UpdateFirmware");
        assertThat(cmd.payload()).containsEntry("location", "ftp://example.com/fw.bin");
        assertThat(cmd.payload()).containsEntry("retrieveDate", "2025-06-01T00:00:00Z");
        assertThat(cmd.payload()).containsEntry("retries", 3);
        assertThat(cmd.payload()).containsEntry("retryInterval", 120);
    }

    @Test
    void sends_without_optional_params() {
        commandSender.setNextResponse(Map.of());

        useCase.updateFirmware(tenantId, station,
                "ftp://example.com/fw.bin", "2025-06-01T00:00:00Z", null, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("retries");
        assertThat(cmd.payload()).doesNotContainKey("retryInterval");
    }
}
