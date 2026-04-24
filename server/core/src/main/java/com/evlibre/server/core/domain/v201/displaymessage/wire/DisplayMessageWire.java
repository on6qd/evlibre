package com.evlibre.server.core.domain.v201.displaymessage.wire;

import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.wire.DeviceModelWire;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.displaymessage.MessageState;
import com.evlibre.server.core.domain.v201.model.MessageContent;
import com.evlibre.server.core.domain.v201.model.MessageFormat;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire-format codec for OCPP 2.0.1 Display Message types.
 *
 * <p>Covers {@code MessageInfo}, {@code MessagePriority}, {@code MessageState}
 * and the nested {@link MessageContent}. The {@code MessageContent} codec
 * duplicates {@code IdTokenWire}'s private helper for now — if a third
 * caller emerges the two can consolidate into a shared {@code MessageWire}.
 */
public final class DisplayMessageWire {

    private DisplayMessageWire() {}

    public static String priorityToWire(MessagePriority p) {
        return switch (p) {
            case ALWAYS_FRONT -> "AlwaysFront";
            case IN_FRONT -> "InFront";
            case NORMAL_CYCLE -> "NormalCycle";
        };
    }

    public static MessagePriority priorityFromWire(String wire) {
        return switch (wire) {
            case "AlwaysFront" -> MessagePriority.ALWAYS_FRONT;
            case "InFront" -> MessagePriority.IN_FRONT;
            case "NormalCycle" -> MessagePriority.NORMAL_CYCLE;
            default -> throw new IllegalArgumentException(
                    "Unknown MessagePriority wire value: " + wire);
        };
    }

    public static String stateToWire(MessageState s) {
        return switch (s) {
            case CHARGING -> "Charging";
            case FAULTED -> "Faulted";
            case IDLE -> "Idle";
            case UNAVAILABLE -> "Unavailable";
        };
    }

    public static MessageState stateFromWire(String wire) {
        return switch (wire) {
            case "Charging" -> MessageState.CHARGING;
            case "Faulted" -> MessageState.FAULTED;
            case "Idle" -> MessageState.IDLE;
            case "Unavailable" -> MessageState.UNAVAILABLE;
            default -> throw new IllegalArgumentException(
                    "Unknown MessageState wire value: " + wire);
        };
    }

    public static Map<String, Object> messageContentToWire(MessageContent m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("format", messageFormatToWire(m.format()));
        if (m.language() != null) {
            out.put("language", m.language());
        }
        out.put("content", m.content());
        return out;
    }

    @SuppressWarnings("unchecked")
    public static MessageContent messageContentFromWire(Map<String, Object> wire) {
        MessageFormat format = messageFormatFromWire((String) wire.get("format"));
        String language = (String) wire.get("language");
        String content = (String) wire.get("content");
        return new MessageContent(format, language, content);
    }

    public static String messageFormatToWire(MessageFormat f) {
        return switch (f) {
            case ASCII -> "ASCII";
            case HTML -> "HTML";
            case URI -> "URI";
            case UTF8 -> "UTF8";
        };
    }

    public static MessageFormat messageFormatFromWire(String wire) {
        return switch (wire) {
            case "ASCII" -> MessageFormat.ASCII;
            case "HTML" -> MessageFormat.HTML;
            case "URI" -> MessageFormat.URI;
            case "UTF8" -> MessageFormat.UTF8;
            default -> throw new IllegalArgumentException(
                    "Unknown MessageFormat wire value: " + wire);
        };
    }

    public static Map<String, Object> messageInfoToWire(MessageInfo info) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", info.id());
        out.put("priority", priorityToWire(info.priority()));
        out.put("message", messageContentToWire(info.message()));
        if (info.state() != null) {
            out.put("state", stateToWire(info.state()));
        }
        if (info.display() != null) {
            out.put("display", DeviceModelWire.componentToWire(info.display()));
        }
        if (info.startDateTime() != null) {
            out.put("startDateTime", DateTimeFormatter.ISO_INSTANT.format(info.startDateTime()));
        }
        if (info.endDateTime() != null) {
            out.put("endDateTime", DateTimeFormatter.ISO_INSTANT.format(info.endDateTime()));
        }
        if (info.transactionId() != null) {
            out.put("transactionId", info.transactionId());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static MessageInfo messageInfoFromWire(Map<String, Object> wire) {
        int id = ((Number) wire.get("id")).intValue();
        MessagePriority priority = priorityFromWire((String) wire.get("priority"));
        MessageContent message = messageContentFromWire((Map<String, Object>) wire.get("message"));
        MessageState state = wire.containsKey("state")
                ? stateFromWire((String) wire.get("state")) : null;
        Component display = wire.containsKey("display")
                ? DeviceModelWire.componentFromWire((Map<String, Object>) wire.get("display"))
                : null;
        Instant startDateTime = wire.containsKey("startDateTime")
                ? Instant.parse((String) wire.get("startDateTime")) : null;
        Instant endDateTime = wire.containsKey("endDateTime")
                ? Instant.parse((String) wire.get("endDateTime")) : null;
        String transactionId = (String) wire.get("transactionId");
        return new MessageInfo(id, priority, message, state, display,
                startDateTime, endDateTime, transactionId);
    }
}
