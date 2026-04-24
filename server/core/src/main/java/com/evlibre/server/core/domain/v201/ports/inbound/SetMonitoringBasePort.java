package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.MonitoringBase;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringBaseResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetMonitoringBase} (OCPP 2.0.1 N01 — Set Monitoring
 * Base).
 *
 * <p>Switches the station between its monitoring-configuration sets. Returns
 * a {@link SetMonitoringBaseResult} — may be {@code NotSupported} when the
 * station does not implement {@code MonitoringCtrlr}.
 */
public interface SetMonitoringBasePort {

    CompletableFuture<SetMonitoringBaseResult> setMonitoringBase(TenantId tenantId,
                                                                   ChargePointIdentity stationIdentity,
                                                                   MonitoringBase monitoringBase);
}
