package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.PublishFirmwareResult;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for sending an OCPP 2.0.1 {@code PublishFirmware} call (block
 * L03). The CSMS instructs a Local Controller to download a firmware image
 * and republish it on the local network so other charging stations can
 * retrieve it without each calling out to the CSMS.
 *
 * <p>{@code checksum} is a 32-character hex MD5 of the firmware image. Per
 * L03.FR.05 the Local Controller MUST validate the checksum after download.
 */
public interface PublishFirmwarePort {

    CompletableFuture<PublishFirmwareResult> publishFirmware(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            String location,
            String checksum,
            Integer retries,
            Integer retryInterval);
}
