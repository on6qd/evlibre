package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.security.SecurityEvent;

/**
 * Inbound port for {@code SecurityEventNotificationRequest} (block A03).
 * The station reports a single security-relevant event (tamper detection,
 * invalid CSMS certificate, firmware update failed, etc). The CSMS replies
 * with an empty acknowledgement.
 */
public interface HandleSecurityEventNotificationPort {

    void handleSecurityEventNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            SecurityEvent event);
}
