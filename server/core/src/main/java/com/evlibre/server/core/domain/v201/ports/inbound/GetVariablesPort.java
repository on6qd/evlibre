package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.GetVariableResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetVariables} (OCPP 2.0.1 B06 — Get Variables).
 *
 * <p>Unlike {@code GetBaseReport} / {@code GetReport}, this exchange is
 * synchronous: the response carries one {@link GetVariableResult} per
 * {@link GetVariableData} request entry, with the variable's current value
 * inline (when accepted). No {@code NotifyReport} follow-up is sent.
 *
 * <p>The station enforces per-message limits via the
 * {@code DeviceDataCtrlr.ItemsPerMessage.GetVariables} and
 * {@code DeviceDataCtrlr.BytesPerMessage.GetVariables} variables; the caller is
 * responsible for staying within them.
 */
public interface GetVariablesPort {

    CompletableFuture<List<GetVariableResult>> getVariables(TenantId tenantId,
                                                             ChargePointIdentity stationIdentity,
                                                             List<GetVariableData> requests);
}
