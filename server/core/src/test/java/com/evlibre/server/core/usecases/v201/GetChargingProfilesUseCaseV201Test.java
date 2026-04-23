package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesResult;
import com.evlibre.server.core.domain.v201.dto.GetChargingProfilesStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileCriterion;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetChargingProfilesUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetChargingProfilesUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetChargingProfilesUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void payload_with_empty_criterion_omits_optional_fields() {
        useCase.getChargingProfiles(tenantId, station, 101, null, ChargingProfileCriterion.all()).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetChargingProfiles");
        assertThat(cmd.payload())
                .containsEntry("requestId", 101)
                .doesNotContainKey("evseId");

        @SuppressWarnings("unchecked")
        Map<String, Object> criterionWire = (Map<String, Object>) cmd.payload().get("chargingProfile");
        assertThat(criterionWire).isEmpty();
    }

    @Test
    void payload_with_evse_id_zero_serialised() {
        useCase.getChargingProfiles(tenantId, station, 42, 0, ChargingProfileCriterion.all()).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("evseId", 0);
    }

    @Test
    void payload_with_full_criterion_serialises_arrays_and_scalars() {
        ChargingProfileCriterion c = new ChargingProfileCriterion(
                List.of(ChargingLimitSource.EMS, ChargingLimitSource.CSO),
                List.of(1, 2, 3),
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                2);

        useCase.getChargingProfiles(tenantId, station, 7, 5, c).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("evseId", 5);

        @SuppressWarnings("unchecked")
        Map<String, Object> criterionWire = (Map<String, Object>) cmd.payload().get("chargingProfile");
        assertThat(criterionWire)
                .containsEntry("chargingProfilePurpose", "TxDefaultProfile")
                .containsEntry("stackLevel", 2);
        @SuppressWarnings("unchecked")
        List<Object> sources = (List<Object>) criterionWire.get("chargingLimitSource");
        assertThat(sources).containsExactly("EMS", "CSO");
        @SuppressWarnings("unchecked")
        List<Object> ids = (List<Object>) criterionWire.get("chargingProfileId");
        assertThat(ids).containsExactly(1, 2, 3);
    }

    @Test
    void accepted_status_parsed() {
        GetChargingProfilesResult r = useCase.getChargingProfiles(
                tenantId, station, 1, null, ChargingProfileCriterion.all()).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GetChargingProfilesStatus.ACCEPTED);
    }

    @Test
    void no_profiles_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "NoProfiles"));

        GetChargingProfilesResult r = useCase.getChargingProfiles(
                tenantId, station, 1, null, ChargingProfileCriterion.all()).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(GetChargingProfilesStatus.NO_PROFILES);
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.getChargingProfiles(
                tenantId, station, 1, null, ChargingProfileCriterion.all()).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void negative_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.getChargingProfiles(
                tenantId, station, 1, -1, ChargingProfileCriterion.all()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void limit_source_array_cap_enforced() {
        assertThatThrownBy(() -> new ChargingProfileCriterion(
                List.of(ChargingLimitSource.EMS, ChargingLimitSource.SO,
                        ChargingLimitSource.CSO, ChargingLimitSource.OTHER, ChargingLimitSource.EMS),
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 4");
    }
}
