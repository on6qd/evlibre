package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.server.core.domain.v201.smartcharging.CompositeSchedule;

import java.util.Map;
import java.util.Objects;

/**
 * Typed result of a {@code GetCompositeSchedule} call. When {@link #status}
 * is {@link GenericStatus#ACCEPTED}, {@link #schedule} is present; when
 * {@link GenericStatus#REJECTED} (K08.FR.05 / FR.07 — unknown EVSE or
 * unconfigured rate unit), the station omits the schedule and typically
 * sets {@code statusInfo.reasonCode}.
 */
public record GetCompositeScheduleResult(
        GenericStatus status,
        CompositeSchedule schedule,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public GetCompositeScheduleResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public boolean isAccepted() {
        return status == GenericStatus.ACCEPTED;
    }
}
