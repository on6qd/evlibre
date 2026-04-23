package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;

import java.util.concurrent.CompletableFuture;

public interface SendDataTransferPort {

    CompletableFuture<DataTransferResult> sendDataTransfer(TenantId tenantId,
                                                             ChargePointIdentity stationIdentity,
                                                             String vendorId,
                                                             String messageId,
                                                             Object data);
}
