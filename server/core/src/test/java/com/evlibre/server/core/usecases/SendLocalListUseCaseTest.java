package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SendLocalListUseCaseTest {

    private StubCommandSender commandSender;
    private SendLocalListUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new SendLocalListUseCase(commandSender);
    }

    @Test
    void sends_full_list() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        List<Map<String, Object>> authList = List.of(
                Map.of("idTag", "TAG001", "idTagInfo", Map.of("status", "Accepted")),
                Map.of("idTag", "TAG002", "idTagInfo", Map.of("status", "Accepted"))
        );

        CommandResult result = useCase.sendLocalList(tenantId, station, 1, "Full", authList).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("SendLocalList");
        assertThat(cmd.payload()).containsEntry("listVersion", 1);
        assertThat(cmd.payload()).containsEntry("updateType", "Full");
        assertThat(cmd.payload()).containsKey("localAuthorizationList");
    }

    @Test
    void sends_differential_without_list() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.sendLocalList(tenantId, station, 2, "Differential", null).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("localAuthorizationList");
        assertThat(cmd.payload()).containsEntry("updateType", "Differential");
    }
}
