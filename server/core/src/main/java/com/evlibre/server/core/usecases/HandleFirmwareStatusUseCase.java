package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.HandleFirmwareStatusPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleFirmwareStatusUseCase implements HandleFirmwareStatusPort {

    private static final Logger log = LoggerFactory.getLogger(HandleFirmwareStatusUseCase.class);

    private final OcppEventLogPort eventLog;

    public HandleFirmwareStatusUseCase(OcppEventLogPort eventLog) {
        this.eventLog = Objects.requireNonNull(eventLog);
    }

    @Override
    public void handleFirmwareStatus(TenantId tenantId, ChargePointIdentity stationIdentity, String status) {
        log.info("FirmwareStatusNotification from {} (tenant: {}): {}",
                stationIdentity.value(), tenantId.value(), status);
        eventLog.logEvent(tenantId.value(), stationIdentity.value(),
                "FirmwareStatusNotification", "CS->CSMS", "status=" + status);
    }
}
