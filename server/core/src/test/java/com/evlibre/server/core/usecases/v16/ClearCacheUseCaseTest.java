package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClearCacheUseCaseTest {

    private StubCommandSender commandSender;
    private ClearCacheUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new ClearCacheUseCase(commandSender);
    }

    @Test
    void sends_empty_payload() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.clearCache(tenantId, station).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ClearCache");
        assertThat(cmd.payload()).isEmpty();
    }
}
