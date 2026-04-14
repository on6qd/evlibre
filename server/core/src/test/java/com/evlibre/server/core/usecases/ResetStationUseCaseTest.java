package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ResetStationUseCaseTest {

    private StubCommandSender commandSender;
    private ResetStationUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new ResetStationUseCase(commandSender);
    }

    @Test
    void hard_reset_sends_correct_payload() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.reset(tenantId, station, "Hard").join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(commandSender.commands()).hasSize(1);
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("Reset");
        assertThat(cmd.payload()).containsEntry("type", "Hard");
    }

    @Test
    void soft_reset_sends_correct_payload() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.reset(tenantId, station, "Soft").join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("type", "Soft");
    }

    @Test
    void rejected_reset_returns_rejected() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.reset(tenantId, station, "Hard").join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo("Rejected");
    }
}
