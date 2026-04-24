package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyDisplayMessagesPort;
import com.evlibre.server.core.domain.v201.ports.outbound.DisplayMessagesSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates {@code NotifyDisplayMessages} frames per
 * {@code (tenantId, stationIdentity, requestId)} so the chunked
 * {@link MessageInfo} lists arrive at the subscriber as one combined list
 * exactly once.
 *
 * <p>Mirrors {@code HandleNotifyReportUseCaseV201}'s frame-driven contract,
 * adapted to the (narrower) NotifyDisplayMessages schema that carries no
 * {@code seqNo}/{@code generatedAt} — the station still guarantees frame
 * order on the session, so the use case just appends as frames arrive.
 */
public class HandleNotifyDisplayMessagesUseCaseV201 implements HandleNotifyDisplayMessagesPort {

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyDisplayMessagesUseCaseV201.class);

    private final DisplayMessagesSink sink;

    private final Map<RequestKey, List<MessageInfo>> buffers = new ConcurrentHashMap<>();

    public HandleNotifyDisplayMessagesUseCaseV201(DisplayMessagesSink sink) {
        this.sink = Objects.requireNonNull(sink);
    }

    @Override
    public void handleFrame(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            int requestId,
                            boolean tbc,
                            List<MessageInfo> messages) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(messages);

        RequestKey key = new RequestKey(tenantId, stationIdentity, requestId);

        if (tbc) {
            buffers.merge(key, new ArrayList<>(messages), (existing, incoming) -> {
                existing.addAll(incoming);
                return existing;
            });
            log.debug("Buffered NotifyDisplayMessages frame from {} (requestId={}, messages={})",
                    stationIdentity.value(), requestId, messages.size());
            return;
        }

        List<MessageInfo> buffered = buffers.remove(key);
        List<MessageInfo> combined = (buffered != null)
                ? buffered
                : new ArrayList<>(messages.size());
        combined.addAll(messages);

        sink.onDisplayMessages(tenantId, stationIdentity, requestId, List.copyOf(combined));

        log.info("NotifyDisplayMessages complete from {} (requestId={}, totalMessages={})",
                stationIdentity.value(), requestId, combined.size());
    }

    private record RequestKey(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
}
