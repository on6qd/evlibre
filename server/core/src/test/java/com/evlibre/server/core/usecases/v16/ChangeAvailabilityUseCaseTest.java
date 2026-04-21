package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeAvailabilityUseCaseTest {

    private StubCommandSender commandSender;
    private ChangeAvailabilityUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new ChangeAvailabilityUseCase(commandSender);
    }

    @Test
    void sends_inoperative_request() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.changeAvailability(tenantId, station, 1, "Inoperative").join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ChangeAvailability");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
        assertThat(cmd.payload()).containsEntry("type", "Inoperative");
    }

    @Test
    void scheduled_response_is_not_accepted() {
        commandSender.setNextResponse(Map.of("status", "Scheduled"));

        CommandResult result = useCase.changeAvailability(tenantId, station, 0, "Operative").join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo("Scheduled");
    }
}
