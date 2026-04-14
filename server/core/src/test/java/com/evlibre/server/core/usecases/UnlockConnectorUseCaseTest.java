package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnlockConnectorUseCaseTest {

    private StubCommandSender commandSender;
    private UnlockConnectorUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new UnlockConnectorUseCase(commandSender);
    }

    @Test
    void sends_correct_connectorId() {
        commandSender.setNextResponse(Map.of("status", "Unlocked"));

        CommandResult result = useCase.unlockConnector(tenantId, station, 1).join();

        assertThat(result.status()).isEqualTo("Unlocked");
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("UnlockConnector");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
    }
}
