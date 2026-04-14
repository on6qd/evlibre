package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.HandleDataTransferPort;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleDataTransferUseCase implements HandleDataTransferPort {

    private static final Logger log = LoggerFactory.getLogger(HandleDataTransferUseCase.class);

    private final OcppEventLogPort eventLog;

    public HandleDataTransferUseCase(OcppEventLogPort eventLog) {
        this.eventLog = Objects.requireNonNull(eventLog);
    }

    @Override
    public CommandResult handleDataTransfer(TenantId tenantId, String vendorId, String messageId, String data) {
        log.info("DataTransfer received (tenant: {}, vendor: {}, messageId: {})", tenantId.value(), vendorId, messageId);
        eventLog.logEvent(tenantId.value(), "DataTransfer", "DataTransfer", "CS->CSMS",
                String.format("vendorId=%s, messageId=%s", vendorId, messageId));
        return new CommandResult("Accepted");
    }
}
