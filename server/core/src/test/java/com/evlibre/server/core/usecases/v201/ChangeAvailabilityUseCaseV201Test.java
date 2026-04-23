package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityResult;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityStatus;
import com.evlibre.server.core.domain.v201.dto.OperationalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeAvailabilityUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ChangeAvailabilityUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ChangeAvailabilityUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void station_level_payload_has_no_evse_field() {
        useCase.changeAvailability(tenantId, station, OperationalStatus.INOPERATIVE, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ChangeAvailability");
        assertThat(cmd.payload())
                .containsEntry("operationalStatus", "Inoperative")
                .doesNotContainKey("evse");
    }

    @Test
    void evse_level_payload_omits_connector_id() {
        useCase.changeAvailability(tenantId, station, OperationalStatus.OPERATIVE, Evse.of(2)).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("operationalStatus", "Operative");
        assertThat(cmd.payload().get("evse"))
                .isInstanceOfSatisfying(Map.class, evse -> {
                    assertThat(evse).containsEntry("id", 2);
                    assertThat(evse).doesNotContainKey("connectorId");
                });
    }

    @Test
    void connector_level_payload_includes_both_ids() {
        useCase.changeAvailability(tenantId, station, OperationalStatus.INOPERATIVE, Evse.of(2, 1)).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload().get("evse"))
                .isInstanceOfSatisfying(Map.class, evse -> {
                    assertThat(evse).containsEntry("id", 2);
                    assertThat(evse).containsEntry("connectorId", 1);
                });
    }

    @Test
    void accepted_status_parsed() {
        ChangeAvailabilityResult r = useCase
                .changeAvailability(tenantId, station, OperationalStatus.OPERATIVE, null).join();

        assertThat(r.status()).isEqualTo(ChangeAvailabilityStatus.ACCEPTED);
        assertThat(r.isAccepted()).isTrue();
        assertThat(r.isScheduled()).isFalse();
    }

    @Test
    void rejected_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));

        ChangeAvailabilityResult r = useCase
                .changeAvailability(tenantId, station, OperationalStatus.OPERATIVE, null).join();

        assertThat(r.status()).isEqualTo(ChangeAvailabilityStatus.REJECTED);
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void scheduled_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Scheduled"));

        ChangeAvailabilityResult r = useCase
                .changeAvailability(tenantId, station, OperationalStatus.INOPERATIVE, Evse.of(1)).join();

        assertThat(r.status()).isEqualTo(ChangeAvailabilityStatus.SCHEDULED);
        assertThat(r.isScheduled()).isTrue();
    }

    @Test
    void status_info_reason_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "OccupiedByTransaction")));

        ChangeAvailabilityResult r = useCase
                .changeAvailability(tenantId, station, OperationalStatus.INOPERATIVE, Evse.of(1, 1)).join();

        assertThat(r.status()).isEqualTo(ChangeAvailabilityStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("OccupiedByTransaction");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() ->
                useCase.changeAvailability(tenantId, station, OperationalStatus.OPERATIVE, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void null_operational_status_rejected() {
        assertThatThrownBy(() ->
                useCase.changeAvailability(tenantId, station, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("operationalStatus");
    }

    @Test
    void evse_id_zero_rejected_for_this_command() {
        // Evse.of(0) is legal as a Device Model locator (ChargingStation-level
        // component) but not as a ChangeAvailability target — EVSEType.id must be > 0.
        assertThatThrownBy(() ->
                useCase.changeAvailability(tenantId, station, OperationalStatus.OPERATIVE, new Evse(0, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evse.id");
    }
}
