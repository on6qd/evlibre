package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.GetDisplayMessagesResult;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.displaymessage.MessageState;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetDisplayMessages} (OCPP 2.0.1 O02 — Get Display
 * Messages). Requests the station stream back its currently installed
 * display messages via one or more {@code NotifyDisplayMessages} calls
 * keyed on the same {@code requestId}.
 *
 * <p>All three filter parameters are optional: an empty {@code ids} list
 * means "any id"; {@code priority=null} and {@code state=null} mean "any
 * priority" / "any state". When all three are absent the station should
 * return all installed messages.
 */
public interface GetDisplayMessagesPort {

    CompletableFuture<GetDisplayMessagesResult> getDisplayMessages(TenantId tenantId,
                                                                    ChargePointIdentity stationIdentity,
                                                                    int requestId,
                                                                    List<Integer> ids,
                                                                    MessagePriority priority,
                                                                    MessageState state);
}
