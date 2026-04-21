package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.HandleDiagnosticsStatusPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HandleDiagnosticsStatusUseCase implements HandleDiagnosticsStatusPort {

    private static final Logger log = LoggerFactory.getLogger(HandleDiagnosticsStatusUseCase.class);

    private final OcppEventLogPort eventLog;

    public HandleDiagnosticsStatusUseCase(OcppEventLogPort eventLog) {
        this.eventLog = Objects.requireNonNull(eventLog);
    }

    @Override
    public void handleDiagnosticsStatus(TenantId tenantId, ChargePointIdentity stationIdentity, String status) {
        log.info("DiagnosticsStatusNotification from {} (tenant: {}): {}",
                stationIdentity.value(), tenantId.value(), status);
        eventLog.logEvent(tenantId.value(), stationIdentity.value(),
                "DiagnosticsStatusNotification", "CS->CSMS", "status=" + status);
    }
}
