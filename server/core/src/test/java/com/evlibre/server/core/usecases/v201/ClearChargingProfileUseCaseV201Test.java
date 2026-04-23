package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileResult;
import com.evlibre.server.core.domain.v201.dto.ClearChargingProfileStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ClearChargingProfileCriterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClearChargingProfileUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ClearChargingProfileUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ClearChargingProfileUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void clear_by_id_only_omits_criterion() {
        useCase.clearChargingProfile(tenantId, station, 42, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ClearChargingProfile");
        assertThat(cmd.payload())
                .containsEntry("chargingProfileId", 42)
                .doesNotContainKey("chargingProfileCriteria");
    }

    @Test
    void clear_by_criterion_only_omits_id() {
        ClearChargingProfileCriterion c = new ClearChargingProfileCriterion(
                1, ChargingProfilePurpose.TX_DEFAULT_PROFILE, 3);

        useCase.clearChargingProfile(tenantId, station, null, c).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("chargingProfileId");

        @SuppressWarnings("unchecked")
        Map<String, Object> criterionWire = (Map<String, Object>) cmd.payload().get("chargingProfileCriteria");
        assertThat(criterionWire)
                .containsEntry("evseId", 1)
                .containsEntry("chargingProfilePurpose", "TxDefaultProfile")
                .containsEntry("stackLevel", 3);
    }

    @Test
    void evse_id_zero_serialised_not_omitted() {
        ClearChargingProfileCriterion c = new ClearChargingProfileCriterion(0, null, null);

        useCase.clearChargingProfile(tenantId, station, null, c).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> criterionWire = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("chargingProfileCriteria");
        assertThat(criterionWire)
                .containsEntry("evseId", 0)
                .doesNotContainKeys("chargingProfilePurpose", "stackLevel");
    }

    @Test
    void partial_criterion_serialises_only_set_fields() {
        ClearChargingProfileCriterion c = new ClearChargingProfileCriterion(
                null, ChargingProfilePurpose.TX_PROFILE, null);

        useCase.clearChargingProfile(tenantId, station, null, c).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> criterionWire = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("chargingProfileCriteria");
        assertThat(criterionWire)
                .hasSize(1)
                .containsEntry("chargingProfilePurpose", "TxProfile");
    }

    @Test
    void id_and_criterion_both_accepted() {
        ClearChargingProfileCriterion c = new ClearChargingProfileCriterion(
                null, ChargingProfilePurpose.TX_PROFILE, null);

        useCase.clearChargingProfile(tenantId, station, 99, c).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("chargingProfileId", 99);
        assertThat(cmd.payload()).containsKey("chargingProfileCriteria");
    }

    @Test
    void no_id_and_empty_criterion_rejected() {
        assertThatThrownBy(() -> useCase.clearChargingProfile(tenantId, station, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("K10.FR.02");
        ClearChargingProfileCriterion empty = new ClearChargingProfileCriterion(null, null, null);
        assertThatThrownBy(() -> useCase.clearChargingProfile(tenantId, station, null, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("K10.FR.02");
    }

    @Test
    void accepted_status_parsed() {
        ClearChargingProfileResult r = useCase.clearChargingProfile(tenantId, station, 1, null).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(ClearChargingProfileStatus.ACCEPTED);
    }

    @Test
    void unknown_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Unknown"));

        ClearChargingProfileResult r = useCase.clearChargingProfile(tenantId, station, 999, null).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(ClearChargingProfileStatus.UNKNOWN);
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.clearChargingProfile(tenantId, station, 1, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void negative_evse_id_on_criterion_rejected_at_construction() {
        assertThatThrownBy(() -> new ClearChargingProfileCriterion(-1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void negative_stack_level_on_criterion_rejected_at_construction() {
        assertThatThrownBy(() -> new ClearChargingProfileCriterion(null, null, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stackLevel");
    }
}
