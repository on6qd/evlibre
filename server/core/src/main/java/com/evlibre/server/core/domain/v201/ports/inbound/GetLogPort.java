package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.diagnostics.LogParameters;
import com.evlibre.server.core.domain.v201.diagnostics.LogType;
import com.evlibre.server.core.domain.v201.dto.GetLogResult;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for sending an OCPP 2.0.1 {@code GetLog} call (block N01).
 * The CSMS asks the station to upload a log file to the supplied
 * {@link LogParameters#remoteLocation()}. The {@code requestId} ties the
 * call to subsequent {@code LogStatusNotification} updates.
 */
public interface GetLogPort {

    CompletableFuture<GetLogResult> getLog(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            LogType logType,
            int requestId,
            LogParameters log,
            Integer retries,
            Integer retryInterval);
}
