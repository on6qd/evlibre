package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.ChangeAvailabilityResult;
import com.evlibre.server.core.domain.v201.dto.OperationalStatus;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ChangeAvailability} for OCPP 2.0.1 (Functional Block G,
 * use cases G03 and G04).
 *
 * <p>Requests the Charging Station to set the operational state of the station
 * as a whole, a whole EVSE, or a specific connector on an EVSE:
 *
 * <ul>
 *   <li>{@code evse == null} — target the whole Charging Station (G04).</li>
 *   <li>{@code evse.id > 0, connectorId == null} — target an entire EVSE (G03).</li>
 *   <li>{@code evse.id > 0, connectorId > 0} — target a specific connector (G03).</li>
 * </ul>
 *
 * <p>If a transaction is in progress on the target, the station returns
 * {@code Scheduled} and defers the change until the transaction ends
 * (G03.FR.05 / G04.FR.06). The availability state is persistent across
 * reboot/power loss (G03.FR.08 / G04.FR.09).
 */
public interface ChangeAvailabilityPort {

    CompletableFuture<ChangeAvailabilityResult> changeAvailability(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            OperationalStatus operationalStatus,
            Evse evse);
}
