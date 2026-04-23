package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;

public interface HandleDataTransferPort {

    DataTransferResult handleDataTransfer(TenantId tenantId, String vendorId, String messageId, Object data);
}
