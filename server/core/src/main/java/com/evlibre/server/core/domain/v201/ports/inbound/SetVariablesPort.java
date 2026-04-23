package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableData;
import com.evlibre.server.core.domain.v201.devicemodel.SetVariableResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetVariables} (OCPP 2.0.1 B05 — Set Variables).
 *
 * <p>Synchronous: the response carries one {@link SetVariableResult} per
 * {@link SetVariableData} entry. Entries may individually report
 * {@link com.evlibre.server.core.domain.v201.devicemodel.SetVariableStatus#REBOOT_REQUIRED}
 * — the station has accepted and stored the value but only applies it on the
 * next reset, which the CSMS must schedule separately.
 *
 * <p>Per-message caps are exposed by the station through
 * {@code DeviceDataCtrlr.ItemsPerMessage.SetVariables} and
 * {@code DeviceDataCtrlr.BytesPerMessage.SetVariables}; the caller is
 * responsible for staying within them.
 */
public interface SetVariablesPort {

    CompletableFuture<List<SetVariableResult>> setVariables(TenantId tenantId,
                                                             ChargePointIdentity stationIdentity,
                                                             List<SetVariableData> updates);
}
