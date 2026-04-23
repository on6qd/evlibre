package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.firmware.PublishFirmwareStatus;

import java.util.List;

/**
 * Inbound port for {@code PublishFirmwareStatusNotificationRequest} (block
 * L03). The Local Controller reports progress through the publish lifecycle
 * triggered by a prior {@code PublishFirmware}.
 *
 * <p>{@code locations} is non-empty only when {@code status} is
 * {@link PublishFirmwareStatus#PUBLISHED} (L03.FR.04 — must list every URI
 * where the firmware is available, one per supported protocol). For every
 * other status the list is empty. {@code requestId} is optional when the
 * notification is triggered while no publish is ongoing (L03.FR.10).
 */
public interface HandlePublishFirmwareStatusNotificationPort {

    void handlePublishFirmwareStatusNotification(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            PublishFirmwareStatus status,
            List<String> locations,
            Integer requestId);
}
