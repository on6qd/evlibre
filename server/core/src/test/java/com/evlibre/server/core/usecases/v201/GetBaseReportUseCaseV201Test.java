package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetBaseReportUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetBaseReportUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetBaseReportUseCaseV201(commandSender);
    }

    @Test
    void full_inventory_sends_correct_payload() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.getBaseReport(tenantId, station, 42, ReportBase.FULL_INVENTORY).join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(commandSender.commands()).hasSize(1);
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetBaseReport");
        assertThat(cmd.payload())
                .containsEntry("requestId", 42)
                .containsEntry("reportBase", "FullInventory");
    }

    @Test
    void configuration_inventory_maps_to_wire_value() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getBaseReport(tenantId, station, 1, ReportBase.CONFIGURATION_INVENTORY).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("reportBase", "ConfigurationInventory");
    }

    @Test
    void summary_inventory_maps_to_wire_value() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.getBaseReport(tenantId, station, 7, ReportBase.SUMMARY_INVENTORY).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("reportBase", "SummaryInventory");
    }

    @Test
    void rejected_response_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        CommandResult result = useCase.getBaseReport(tenantId, station, 1, ReportBase.FULL_INVENTORY).join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo("Rejected");
    }

    @Test
    void empty_result_set_status_is_propagated() {
        commandSender.setNextResponse(Map.of("status", "EmptyResultSet"));

        CommandResult result = useCase.getBaseReport(tenantId, station, 1, ReportBase.SUMMARY_INVENTORY).join();

        assertThat(result.status()).isEqualTo("EmptyResultSet");
        assertThat(result.isAccepted()).isFalse();
    }
}
