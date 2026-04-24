package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SetMonitoringLevelResult;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetMonitoringLevel} (OCPP 2.0.1 N02 — Set
 * Monitoring Level).
 *
 * <p>Sets the station-wide severity threshold below which events are reported
 * to the CSMS. Severity values are 0–9 (0 = Danger, 9 = Debug); values
 * outside that range are rejected at the caller boundary.
 */
public interface SetMonitoringLevelPort {

    CompletableFuture<SetMonitoringLevelResult> setMonitoringLevel(TenantId tenantId,
                                                                    ChargePointIdentity stationIdentity,
                                                                    int severity);
}
