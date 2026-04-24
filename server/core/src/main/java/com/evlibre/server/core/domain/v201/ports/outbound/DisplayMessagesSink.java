package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;

import java.util.List;

/**
 * Fires once per completed {@code NotifyDisplayMessages} transmission —
 * the frame with {@code tbc=false} marks the end of the sequence keyed on
 * {@code requestId}. Delivers the concatenated {@link MessageInfo} list
 * across all frames.
 *
 * <p>An empty list is legitimately delivered when the station had nothing
 * matching the CSMS's original GetDisplayMessages filter but still chose to
 * acknowledge with a (tbc=false, no messageInfo) frame.
 */
@FunctionalInterface
public interface DisplayMessagesSink {

    void onDisplayMessages(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            int requestId,
                            List<MessageInfo> messages);
}
