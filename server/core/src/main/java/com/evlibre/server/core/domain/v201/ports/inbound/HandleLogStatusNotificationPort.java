package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.UploadLogStatus;

/**
 * Inbound port for {@code LogStatusNotificationRequest} (N01.FR.08).
 * Station notifies the CSMS of progress through a log-upload lifecycle
 * triggered by a prior {@code GetLog}. {@code requestId} is mandatory unless
 * the message was triggered while no upload is ongoing (N01.FR.13) — callers
 * should treat absent as {@code null}.
 */
public interface HandleLogStatusNotificationPort {

    void handleLogStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            UploadLogStatus status,
            Integer requestId);
}
