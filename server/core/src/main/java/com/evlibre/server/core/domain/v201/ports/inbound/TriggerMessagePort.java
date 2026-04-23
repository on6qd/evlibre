package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.TriggerMessageResult;
import com.evlibre.server.core.domain.v201.model.MessageTrigger;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code TriggerMessage} for OCPP 2.0.1 (use case F06).
 *
 * <p>Asks the station to emit a specific message (e.g. {@code BootNotification},
 * {@code StatusNotification}, {@code SignCertificate}). The station acks
 * synchronously with {@link TriggerMessageResult} and then sends the triggered
 * message as a separate CALL.
 *
 * @param evse optional target. If absent, the station SHALL treat the trigger
 *             as applying to all EVSEs / connectors. Per spec, for
 *             {@link MessageTrigger#STATUS_NOTIFICATION} the CSMS SHALL supply
 *             both {@code evse.id} and {@code evse.connectorId}.
 */
public interface TriggerMessagePort {

    CompletableFuture<TriggerMessageResult> triggerMessage(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            MessageTrigger requestedMessage,
            Evse evse);
}
