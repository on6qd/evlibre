package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteStartTransactionUseCaseTest {

    private StubCommandSender commandSender;
    private RemoteStartTransactionUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new RemoteStartTransactionUseCase(commandSender);
    }

    @Test
    void sends_with_idTag_and_connectorId() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.remoteStart(tenantId, station, "TAG001", 1).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("RemoteStartTransaction");
        assertThat(cmd.payload()).containsEntry("idTag", "TAG001");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
    }

    @Test
    void sends_without_connectorId_when_null() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.remoteStart(tenantId, station, "TAG001", null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("connectorId");
        assertThat(cmd.payload()).containsEntry("idTag", "TAG001");
    }
}
