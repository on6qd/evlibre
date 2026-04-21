package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteStopTransactionUseCaseTest {

    private StubCommandSender commandSender;
    private RemoteStopTransactionUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new RemoteStopTransactionUseCase(commandSender);
    }

    @Test
    void sends_correct_transactionId() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.remoteStop(tenantId, station, 42).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("RemoteStopTransaction");
        assertThat(cmd.payload()).containsEntry("transactionId", 42);
    }
}
