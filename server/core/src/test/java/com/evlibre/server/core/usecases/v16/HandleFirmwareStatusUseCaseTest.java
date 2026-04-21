package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleFirmwareStatusUseCaseTest {

    @Test
    void logs_firmware_status() {
        var eventLog = new RecordingEventLog();
        var useCase = new HandleFirmwareStatusUseCase(eventLog);

        useCase.handleFirmwareStatus(
                new TenantId("demo-tenant"),
                new ChargePointIdentity("CHARGER-001"),
                "Installed");

        assertThat(eventLog.events).hasSize(1);
        assertThat(eventLog.events.get(0)).contains("FirmwareStatusNotification");
    }

    static class RecordingEventLog implements OcppEventLogPort {
        final List<String> events = new ArrayList<>();
        @Override
        public void logEvent(String s, String m, String a, String d, String p) {
            events.add(a + ":" + p);
        }
    }
}
