package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.UpdateFirmwareResult;
import com.evlibre.server.core.domain.v201.firmware.Firmware;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for sending an OCPP 2.0.1 {@code UpdateFirmware} call (L01).
 * The CSMS instructs the station to download a firmware bundle and install it.
 * {@code requestId} correlates this call with subsequent
 * {@code FirmwareStatusNotification} updates from the station (L01.FR.20).
 */
public interface UpdateFirmwarePort {

    CompletableFuture<UpdateFirmwareResult> updateFirmware(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            Firmware firmware,
            Integer retries,
            Integer retryInterval);
}
