package com.evlibre.server.adapter.webui;

import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * Encodes/decodes {@link MessageTraceEntry} values for in-process Vert.x event-bus
 * delivery. Single-JVM deployment, so we accept the JsonObject roundtrip rather than
 * register a custom codec per sealed-type variant.
 */
public final class MessageTraceEntryCodec {

    private static final String KIND_FRAME = "frame";
    private static final String KIND_LIFECYCLE = "lifecycle";

    private MessageTraceEntryCodec() {}

    public static JsonObject encode(MessageTraceEntry entry) {
        if (entry instanceof OcppFrame f) {
            return new JsonObject()
                    .put("kind", KIND_FRAME)
                    .put("timestamp", f.timestamp().toString())
                    .put("direction", f.direction().name())
                    .put("type", f.type().name())
                    .put("action", f.action())
                    .put("messageId", f.messageId())
                    .put("rawPayload", f.rawPayload());
        }
        if (entry instanceof Lifecycle l) {
            return new JsonObject()
                    .put("kind", KIND_LIFECYCLE)
                    .put("timestamp", l.timestamp().toString())
                    .put("lifecycleKind", l.kind().name())
                    .put("detail", l.detail());
        }
        throw new IllegalArgumentException("Unsupported entry type: " + entry.getClass());
    }

    public static MessageTraceEntry decode(JsonObject json) {
        String kind = json.getString("kind");
        Instant timestamp = Instant.parse(json.getString("timestamp"));
        if (KIND_FRAME.equals(kind)) {
            return new OcppFrame(
                    timestamp,
                    Direction.valueOf(json.getString("direction")),
                    FrameType.valueOf(json.getString("type")),
                    json.getString("action"),
                    json.getString("messageId"),
                    json.getString("rawPayload"));
        }
        if (KIND_LIFECYCLE.equals(kind)) {
            return new Lifecycle(
                    timestamp,
                    LifecycleKind.valueOf(json.getString("lifecycleKind")),
                    json.getString("detail"));
        }
        throw new IllegalArgumentException("Unknown kind: " + kind);
    }
}
