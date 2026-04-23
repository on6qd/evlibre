package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ChargingProfileStatus;
import com.evlibre.server.core.domain.v201.dto.SetChargingProfileResult;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedulePeriod;
import com.evlibre.server.core.domain.v201.smartcharging.RecurrencyKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetChargingProfileUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetChargingProfileUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetChargingProfileUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    private static ChargingProfile relativeTxDefault(int id, int stack, double limitA) {
        return new ChargingProfile(
                id, stack,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, null, 3600, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, limitA)), null)));
    }

    @Test
    void payload_wraps_profile_under_chargingProfile_with_evse_id() {
        useCase.setChargingProfile(tenantId, station, 0, relativeTxDefault(1, 0, 16.0)).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("SetChargingProfile");
        assertThat(cmd.payload()).containsEntry("evseId", 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) cmd.payload().get("chargingProfile");
        assertThat(profile)
                .containsEntry("id", 1)
                .containsEntry("stackLevel", 0)
                .containsEntry("chargingProfilePurpose", "TxDefaultProfile")
                .containsEntry("chargingProfileKind", "Relative")
                .doesNotContainKeys("transactionId", "validFrom", "validTo", "recurrencyKind");
    }

    @Test
    void schedule_wire_shape_matches_spec() {
        Instant start = Instant.parse("2027-01-01T00:00:00Z");
        ChargingProfile absoluteWeekly = new ChargingProfile(
                9, 2,
                ChargingProfilePurpose.CHARGING_STATION_MAX_PROFILE,
                ChargingProfileKind.RECURRING,
                RecurrencyKind.WEEKLY,
                start, start.plusSeconds(86400L * 30), null,
                List.of(new ChargingSchedule(7, start, 3600, ChargingRateUnit.WATTS,
                        List.of(
                                ChargingSchedulePeriod.of(0, 22000.0),
                                new ChargingSchedulePeriod(1800, 11000.0, 1, 2)),
                        0.5)));

        useCase.setChargingProfile(tenantId, station, 0, absoluteWeekly).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("chargingProfile");
        assertThat(profile).containsEntry("recurrencyKind", "Weekly")
                .containsEntry("validFrom", "2027-01-01T00:00:00Z");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedules = (List<Map<String, Object>>) profile.get("chargingSchedule");
        assertThat(schedules).hasSize(1);
        Map<String, Object> s = schedules.get(0);
        assertThat(s)
                .containsEntry("chargingRateUnit", "W")
                .containsEntry("startSchedule", "2027-01-01T00:00:00Z")
                .containsEntry("minChargingRate", 0.5);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> periods = (List<Map<String, Object>>) s.get("chargingSchedulePeriod");
        assertThat(periods).hasSize(2);
        assertThat(periods.get(1))
                .containsEntry("startPeriod", 1800)
                .containsEntry("limit", 11000.0)
                .containsEntry("numberPhases", 1)
                .containsEntry("phaseToUse", 2);
    }

    @Test
    void tx_profile_emits_transaction_id_on_wire() {
        ChargingProfile txProfile = new ChargingProfile(
                5, 1,
                ChargingProfilePurpose.TX_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, "tx-42",
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, 32.0)), null)));

        useCase.setChargingProfile(tenantId, station, 2, txProfile).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("chargingProfile");
        assertThat(profile)
                .containsEntry("chargingProfilePurpose", "TxProfile")
                .containsEntry("transactionId", "tx-42");
    }

    @Test
    void accepted_status_parsed() {
        SetChargingProfileResult r = useCase.setChargingProfile(tenantId, station, 0, relativeTxDefault(1, 0, 16.0)).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(ChargingProfileStatus.ACCEPTED);
    }

    @Test
    void rejected_with_reason_code_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "DuplicateProfile")));

        SetChargingProfileResult r = useCase.setChargingProfile(tenantId, station, 0, relativeTxDefault(1, 0, 16.0)).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(ChargingProfileStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("DuplicateProfile");
    }

    @Test
    void max_profile_requires_evse_zero() {
        ChargingProfile max = new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.CHARGING_STATION_MAX_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.WATTS,
                        List.of(ChargingSchedulePeriod.of(0, 22000.0)), null)));

        assertThatThrownBy(() -> useCase.setChargingProfile(tenantId, station, 1, max))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId=0");
    }

    @Test
    void tx_profile_requires_positive_evse_id() {
        ChargingProfile txProfile = new ChargingProfile(
                5, 1,
                ChargingProfilePurpose.TX_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, "tx-42",
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, 32.0)), null)));

        assertThatThrownBy(() -> useCase.setChargingProfile(tenantId, station, 0, txProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId > 0");
    }

    @Test
    void negative_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.setChargingProfile(tenantId, station, -1, relativeTxDefault(1, 0, 16.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.setChargingProfile(tenantId, station, 0, relativeTxDefault(1, 0, 16.0)).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void recurring_without_recurrency_kind_rejected_at_construction() {
        assertThatThrownBy(() -> new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.RECURRING,
                null,
                Instant.parse("2027-01-01T00:00:00Z"), null, null,
                List.of(new ChargingSchedule(1, Instant.parse("2027-01-01T00:00:00Z"), null, ChargingRateUnit.WATTS,
                        List.of(ChargingSchedulePeriod.of(0, 22000.0)), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recurrencyKind");
    }

    @Test
    void relative_with_start_schedule_rejected_at_construction() {
        assertThatThrownBy(() -> new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, Instant.parse("2027-01-01T00:00:00Z"), null, ChargingRateUnit.WATTS,
                        List.of(ChargingSchedulePeriod.of(0, 22000.0)), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Relative");
    }

    @Test
    void absolute_without_start_schedule_rejected_at_construction() {
        assertThatThrownBy(() -> new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.ABSOLUTE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.WATTS,
                        List.of(ChargingSchedulePeriod.of(0, 22000.0)), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startSchedule");
    }

    @Test
    void tx_profile_without_transaction_id_rejected_at_construction() {
        assertThatThrownBy(() -> new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.TX_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, null,
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, 32.0)), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");
    }

    @Test
    void non_tx_profile_with_transaction_id_rejected_at_construction() {
        assertThatThrownBy(() -> new ChargingProfile(
                1, 0,
                ChargingProfilePurpose.TX_DEFAULT_PROFILE,
                ChargingProfileKind.RELATIVE,
                null, null, null, "tx-stowaway",
                List.of(new ChargingSchedule(1, null, null, ChargingRateUnit.AMPERES,
                        List.of(ChargingSchedulePeriod.of(0, 32.0)), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TxProfile");
    }

    @Test
    void first_period_must_start_at_zero() {
        assertThatThrownBy(() -> new ChargingSchedule(1, null, null, ChargingRateUnit.AMPERES,
                List.of(new ChargingSchedulePeriod(60, 16.0, null, null)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startPeriod must be 0");
    }

    @Test
    void phase_to_use_requires_single_phase() {
        assertThatThrownBy(() -> new ChargingSchedulePeriod(0, 16.0, 3, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phaseToUse");
    }
}
