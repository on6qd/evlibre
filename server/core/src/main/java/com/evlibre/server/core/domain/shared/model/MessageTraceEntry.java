package com.evlibre.server.core.domain.shared.model;

import java.time.Instant;

public sealed interface MessageTraceEntry permits MessageTraceEntry.OcppFrame, MessageTraceEntry.Lifecycle {

    Instant timestamp();

    enum Direction { IN, OUT }

    enum FrameType { CALL, CALL_RESULT, CALL_ERROR }

    enum LifecycleKind { CONNECTED, DISCONNECTED, SUBPROTOCOL_REJECTED }

    record OcppFrame(
            Instant timestamp,
            Direction direction,
            FrameType type,
            String action,
            String messageId,
            String rawPayload
    ) implements MessageTraceEntry {}

    record Lifecycle(
            Instant timestamp,
            LifecycleKind kind,
            String detail
    ) implements MessageTraceEntry {}
}
