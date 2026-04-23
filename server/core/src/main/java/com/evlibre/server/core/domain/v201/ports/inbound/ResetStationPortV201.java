package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.model.ResetType;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code Reset} for OCPP 2.0.1 (use cases B11 — reset without
 * ongoing transaction, and B12 — reset with ongoing transaction).
 *
 * <p>Separate from the v1.6 {@code ResetStationPort}: the request shape
 * differs ({@code Immediate}/{@code OnIdle} instead of {@code Hard}/{@code
 * Soft}, plus an optional {@code evseId} for per-EVSE reset), and the
 * response adds a {@code Scheduled} status v1.6 does not have. Sharing a port
 * would require a protocol branch in the use case — explicitly forbidden by
 * the separation rule.
 *
 * @param evseId absent ({@code null}) = whole-station reset; present = reset
 *               only the addressed EVSE. Stations MAY respond {@code Rejected}
 *               if they don't support per-EVSE reset.
 */
public interface ResetStationPortV201 {

    CompletableFuture<CommandResult> reset(TenantId tenantId,
                                            ChargePointIdentity stationIdentity,
                                            ResetType type,
                                            Integer evseId);
}
