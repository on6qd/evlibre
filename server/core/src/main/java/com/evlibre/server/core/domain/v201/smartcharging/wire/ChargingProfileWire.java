package com.evlibre.server.core.domain.v201.smartcharging.wire;

import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedulePeriod;
import com.evlibre.server.core.domain.v201.smartcharging.RecurrencyKind;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire-form codec for the OCPP 2.0.1 Smart Charging structures shared across
 * outbound use cases (SetChargingProfile, GetCompositeSchedule, …) and the
 * inbound ReportChargingProfiles/NotifyChargingLimit handlers.
 *
 * <p>Keeping the wire shape centralised here means the field names and enum
 * spellings stay identical in every direction — mirrors the role
 * {@code DeviceModelWire} plays for the Device Model and {@code IdTokenWire}
 * plays for authorization.
 *
 * <p>Only {@code toWire} directions are filled in as callers need them; the
 * {@code fromWire} direction will be added alongside the first inbound reader
 * of these types.
 */
public final class ChargingProfileWire {

    private ChargingProfileWire() {}

    public static Map<String, Object> toWire(ChargingProfile profile) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", profile.id());
        out.put("stackLevel", profile.stackLevel());
        out.put("chargingProfilePurpose", purposeToWire(profile.chargingProfilePurpose()));
        out.put("chargingProfileKind", kindToWire(profile.chargingProfileKind()));
        if (profile.recurrencyKind() != null) {
            out.put("recurrencyKind", recurrencyKindToWire(profile.recurrencyKind()));
        }
        if (profile.validFrom() != null) {
            out.put("validFrom", DateTimeFormatter.ISO_INSTANT.format(profile.validFrom()));
        }
        if (profile.validTo() != null) {
            out.put("validTo", DateTimeFormatter.ISO_INSTANT.format(profile.validTo()));
        }
        List<Map<String, Object>> schedules = new ArrayList<>(profile.chargingSchedule().size());
        for (ChargingSchedule s : profile.chargingSchedule()) {
            schedules.add(scheduleToWire(s));
        }
        out.put("chargingSchedule", schedules);
        if (profile.transactionId() != null) {
            out.put("transactionId", profile.transactionId());
        }
        return out;
    }

    public static Map<String, Object> scheduleToWire(ChargingSchedule s) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", s.id());
        if (s.startSchedule() != null) {
            out.put("startSchedule", DateTimeFormatter.ISO_INSTANT.format(s.startSchedule()));
        }
        if (s.duration() != null) {
            out.put("duration", s.duration());
        }
        out.put("chargingRateUnit", rateUnitToWire(s.chargingRateUnit()));
        List<Map<String, Object>> periods = new ArrayList<>(s.chargingSchedulePeriod().size());
        for (ChargingSchedulePeriod p : s.chargingSchedulePeriod()) {
            periods.add(periodToWire(p));
        }
        out.put("chargingSchedulePeriod", periods);
        if (s.minChargingRate() != null) {
            out.put("minChargingRate", s.minChargingRate());
        }
        return out;
    }

    public static Map<String, Object> periodToWire(ChargingSchedulePeriod p) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("startPeriod", p.startPeriod());
        out.put("limit", p.limit());
        if (p.numberPhases() != null) {
            out.put("numberPhases", p.numberPhases());
        }
        if (p.phaseToUse() != null) {
            out.put("phaseToUse", p.phaseToUse());
        }
        return out;
    }

    public static String purposeToWire(ChargingProfilePurpose p) {
        return switch (p) {
            case CHARGING_STATION_EXTERNAL_CONSTRAINTS -> "ChargingStationExternalConstraints";
            case CHARGING_STATION_MAX_PROFILE -> "ChargingStationMaxProfile";
            case TX_DEFAULT_PROFILE -> "TxDefaultProfile";
            case TX_PROFILE -> "TxProfile";
        };
    }

    public static String kindToWire(ChargingProfileKind k) {
        return switch (k) {
            case ABSOLUTE -> "Absolute";
            case RECURRING -> "Recurring";
            case RELATIVE -> "Relative";
        };
    }

    public static String recurrencyKindToWire(RecurrencyKind r) {
        return switch (r) {
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
        };
    }

    public static String rateUnitToWire(ChargingRateUnit u) {
        return switch (u) {
            case WATTS -> "W";
            case AMPERES -> "A";
        };
    }
}
