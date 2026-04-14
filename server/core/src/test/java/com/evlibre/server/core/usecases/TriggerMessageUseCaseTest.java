package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerMessageUseCaseTest {

    private StubCommandSender commandSender;
    private TriggerMessageUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new TriggerMessageUseCase(commandSender);
    }

    @Test
    void valid_message_sends_command() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.triggerMessage(tenantId, station, "Heartbeat", null).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("TriggerMessage");
        assertThat(cmd.payload()).containsEntry("requestedMessage", "Heartbeat");
        assertThat(cmd.payload()).doesNotContainKey("connectorId");
    }

    @Test
    void valid_message_with_connector() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.triggerMessage(tenantId, station, "StatusNotification", 1).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
    }

    @Test
    void invalid_message_returns_notImplemented() {
        CommandResult result = useCase.triggerMessage(tenantId, station, "InvalidMessage", null).join();

        assertThat(result.status()).isEqualTo("NotImplemented");
        assertThat(commandSender.commands()).isEmpty();
    }
}
