package com.evlibre.server.core.domain.v201.smartcharging.wire;

import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedulePeriod;
import com.evlibre.server.core.domain.v201.smartcharging.CompositeSchedule;
import com.evlibre.server.core.domain.v201.smartcharging.RecurrencyKind;

import java.time.Instant;
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

    public static String limitSourceToWire(ChargingLimitSource s) {
        return switch (s) {
            case EMS -> "EMS";
            case OTHER -> "Other";
            case SO -> "SO";
            case CSO -> "CSO";
        };
    }

    public static ChargingLimitSource limitSourceFromWire(String wire) {
        return switch (wire) {
            case "EMS" -> ChargingLimitSource.EMS;
            case "Other" -> ChargingLimitSource.OTHER;
            case "SO" -> ChargingLimitSource.SO;
            case "CSO" -> ChargingLimitSource.CSO;
            default -> throw new IllegalArgumentException("Unknown ChargingLimitSource wire value: " + wire);
        };
    }

    public static ChargingRateUnit rateUnitFromWire(String wire) {
        return switch (wire) {
            case "W" -> ChargingRateUnit.WATTS;
            case "A" -> ChargingRateUnit.AMPERES;
            default -> throw new IllegalArgumentException("Unknown ChargingRateUnit wire value: " + wire);
        };
    }

    public static CompositeSchedule compositeScheduleFromWire(Map<?, ?> node) {
        Object evse = node.get("evseId");
        Object dur = node.get("duration");
        Object start = node.get("scheduleStart");
        Object unit = node.get("chargingRateUnit");
        Object periods = node.get("chargingSchedulePeriod");
        if (!(evse instanceof Number) || !(dur instanceof Number) || !(start instanceof String startStr)
                || !(unit instanceof String unitStr) || !(periods instanceof List<?> periodList)) {
            throw new IllegalArgumentException("CompositeSchedule is missing required fields: " + node);
        }
        List<ChargingSchedulePeriod> parsed = new ArrayList<>(periodList.size());
        for (Object p : periodList) {
            if (!(p instanceof Map<?, ?> pMap)) {
                throw new IllegalArgumentException("chargingSchedulePeriod entry is not an object: " + p);
            }
            parsed.add(periodFromWire(pMap));
        }
        return new CompositeSchedule(
                ((Number) evse).intValue(),
                ((Number) dur).intValue(),
                Instant.parse(startStr),
                rateUnitFromWire(unitStr),
                parsed);
    }

    public static ChargingSchedulePeriod periodFromWire(Map<?, ?> node) {
        Object start = node.get("startPeriod");
        Object limit = node.get("limit");
        if (!(start instanceof Number) || !(limit instanceof Number)) {
            throw new IllegalArgumentException("ChargingSchedulePeriod missing startPeriod/limit: " + node);
        }
        Integer numberPhases = node.get("numberPhases") instanceof Number n ? n.intValue() : null;
        Integer phaseToUse = node.get("phaseToUse") instanceof Number n ? n.intValue() : null;
        return new ChargingSchedulePeriod(
                ((Number) start).intValue(),
                ((Number) limit).doubleValue(),
                numberPhases,
                phaseToUse);
    }
}
