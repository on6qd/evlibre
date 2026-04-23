package com.evlibre.server.core.domain.v201.diagnostics;

import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;

import java.time.Instant;
import java.util.Objects;

/**
 * OCPP 2.0.1 {@code EventDataType} — one logical event a station reports
 * inside a {@code NotifyEvent} batch (blocks N07 / N08).
 *
 * <p>Required: {@code eventId}, {@code timestamp}, {@code trigger},
 * {@code actualValue}, {@code eventNotificationType}, {@code component},
 * {@code variable}.
 *
 * <p>Optional clarifiers: {@code cause} (prior eventId that triggered this one),
 * {@code techCode} / {@code techInfo} (vendor-specific diagnostics),
 * {@code cleared} (variable returned to normal), {@code transactionId} (event
 * ties to a specific transaction), {@code variableMonitoringId} (which monitor
 * fired — set when {@code eventNotificationType} is one of the {@code Monitor}
 * variants).
 */
public record EventData(
        int eventId,
        Instant timestamp,
        EventTrigger trigger,
        Integer cause,
        String actualValue,
        String techCode,
        String techInfo,
        Boolean cleared,
        String transactionId,
        Integer variableMonitoringId,
        EventNotificationType eventNotificationType,
        Component component,
        Variable variable) {

    public EventData {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(actualValue, "actualValue");
        Objects.requireNonNull(eventNotificationType, "eventNotificationType");
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(variable, "variable");
        if (actualValue.length() > 2500) {
            throw new IllegalArgumentException(
                    "actualValue exceeds 2500 char limit (" + actualValue.length() + ")");
        }
        if (techCode != null && techCode.length() > 50) {
            throw new IllegalArgumentException(
                    "techCode exceeds 50 char limit (" + techCode.length() + ")");
        }
        if (techInfo != null && techInfo.length() > 500) {
            throw new IllegalArgumentException(
                    "techInfo exceeds 500 char limit (" + techInfo.length() + ")");
        }
        if (transactionId != null && transactionId.length() > 36) {
            throw new IllegalArgumentException(
                    "transactionId exceeds 36 char limit (" + transactionId.length() + ")");
        }
    }
}
