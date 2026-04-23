package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.FirmwareStatus;

/**
 * Inbound port for {@code FirmwareStatusNotificationRequest} (L01.FR.01).
 * Station notifies the CSMS whenever it enters a new state in the firmware
 * download / install lifecycle. {@code requestId} is mandatory unless the
 * status is {@code Idle} (L01.FR.20) — callers should treat absent as
 * {@code null}.
 */
public interface HandleFirmwareStatusNotificationPort {

    void handleFirmwareStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            FirmwareStatus status,
            Integer requestId);
}
