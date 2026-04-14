package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetLocalListVersionUseCaseTest {

    private StubCommandSender commandSender;
    private GetLocalListVersionUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new GetLocalListVersionUseCase(commandSender);
    }

    @Test
    void returns_list_version() {
        commandSender.setNextResponse(Map.of("listVersion", 5));

        CommandResult result = useCase.getLocalListVersion(tenantId, station).join();

        assertThat(result.status()).isEqualTo("5");
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetLocalListVersion");
        assertThat(cmd.payload()).isEmpty();
    }
}
