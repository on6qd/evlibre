package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;

import java.util.List;

/**
 * Accepts a single frame of an OCPP 2.0.1 {@code NotifyDisplayMessages}. The
 * implementation is responsible for per-{@code requestId} aggregation and
 * delivering the assembled list to a sink on the frame with
 * {@code tbc=false}.
 *
 * <p>Unlike {@link HandleNotifyReportPort} the NotifyDisplayMessages schema
 * omits {@code seqNo} and {@code generatedAt} (the inventory is small enough
 * that the spec doesn't require them) — the handleFrame signature is
 * correspondingly narrower.
 */
public interface HandleNotifyDisplayMessagesPort {

    void handleFrame(TenantId tenantId,
                     ChargePointIdentity stationIdentity,
                     int requestId,
                     boolean tbc,
                     List<MessageInfo> messages);
}
