package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;

public interface HandleDataTransferPort {

    CommandResult handleDataTransfer(TenantId tenantId, String vendorId, String messageId, String data);
}
