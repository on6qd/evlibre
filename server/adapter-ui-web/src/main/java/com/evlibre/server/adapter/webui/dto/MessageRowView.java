package com.evlibre.server.adapter.webui.dto;

import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Flat view-model for one row on the Messages tab. Folds the {@link MessageTraceEntry}
 * sealed type into a structure the Rocker template can render with simple booleans
 * (no pattern matching in views).
 */
public record MessageRowView(
        String formattedTime,
        boolean lifecycle,
        // Frame fields (null for lifecycle rows)
        String direction,
        String typeLabel,
        String typeBadgeClass,
        String action,
        String messageId,
        String prettyPayload,
        // Lifecycle fields (null for frame rows)
        String lifecycleLabel
) {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public static MessageRowView from(MessageTraceEntry entry, ObjectMapper mapper) {
        String time = TIME_FMT.format(entry.timestamp());
        if (entry instanceof OcppFrame f) {
            return new MessageRowView(
                    time,
                    false,
                    f.direction() == MessageTraceEntry.Direction.IN ? "→" : "←",
                    typeLabel(f.type()),
                    typeBadgeClass(f.type()),
                    f.action() == null ? "" : f.action(),
                    f.messageId() == null ? "" : f.messageId(),
                    pretty(f.rawPayload(), mapper),
                    null);
        }
        if (entry instanceof Lifecycle l) {
            return new MessageRowView(
                    time,
                    true,
                    null, null, null, null, null, null,
                    lifecycleLabel(l));
        }
        throw new IllegalArgumentException("Unsupported entry: " + entry.getClass());
    }

    private static String typeLabel(MessageTraceEntry.FrameType type) {
        return switch (type) {
            case CALL -> "CALL";
            case CALL_RESULT -> "RESULT";
            case CALL_ERROR -> "ERROR";
        };
    }

    private static String typeBadgeClass(MessageTraceEntry.FrameType type) {
        return switch (type) {
            case CALL -> "badge-info";
            case CALL_RESULT -> "badge-ghost";
            case CALL_ERROR -> "badge-error";
        };
    }

    private static String lifecycleLabel(Lifecycle l) {
        String prefix = switch (l.kind()) {
            case CONNECTED -> "connected";
            case DISCONNECTED -> "disconnected";
            case SUBPROTOCOL_REJECTED -> "sub-protocol rejected";
        };
        return l.detail() == null || l.detail().isBlank()
                ? prefix
                : prefix + " (" + l.detail() + ")";
    }

    private static String pretty(String raw, ObjectMapper mapper) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            Object tree = mapper.readTree(raw);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (Exception e) {
            return raw;
        }
    }
}
