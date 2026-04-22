package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentCriterion;
import com.evlibre.server.core.domain.v201.devicemodel.ComponentVariableSelector;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code GetReport} (OCPP 2.0.1 B08) — Get Custom Report. The
 * caller supplies a {@code requestId}; the station echoes it on every follow-up
 * {@code NotifyReport} frame (spec req. B08.FR.04).
 *
 * <p>Both filter lists are optional; passing an empty set/list omits the
 * respective field on the wire. At most four criteria may be combined per the
 * spec.
 */
public interface GetReportPort {

    CompletableFuture<CommandResult> getReport(TenantId tenantId,
                                                ChargePointIdentity stationIdentity,
                                                int requestId,
                                                Set<ComponentCriterion> criteria,
                                                List<ComponentVariableSelector> selectors);
}
