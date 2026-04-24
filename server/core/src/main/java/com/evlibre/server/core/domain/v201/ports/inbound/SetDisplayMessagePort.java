package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.SetDisplayMessageResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetDisplayMessage} (OCPP 2.0.1 O04 — Set Display
 * Message). Installs or replaces a single message on a station's display
 * surface.
 *
 * <p>Per spec the station owns {@code id} assignment only for new messages
 * via {@code NotifyDisplayMessages}; the CSMS provides a nonnegative id on
 * {@code SetDisplayMessage}, and the station decides whether that id
 * conflicts with an existing one (station-specific policy).
 */
public interface SetDisplayMessagePort {

    CompletableFuture<SetDisplayMessageResult> setDisplayMessage(TenantId tenantId,
                                                                  ChargePointIdentity stationIdentity,
                                                                  MessageInfo message);
}
