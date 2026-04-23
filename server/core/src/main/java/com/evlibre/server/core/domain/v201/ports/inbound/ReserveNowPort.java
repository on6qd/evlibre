package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ReserveNowResult;
import com.evlibre.server.core.domain.v201.model.ConnectorType;
import com.evlibre.server.core.domain.v201.model.IdToken;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code ReserveNow} for OCPP 2.0.1 (use case H01). Asks the
 * station to commit a reservation for a given {@link IdToken}, with an explicit
 * expiry and optionally narrowed to a specific EVSE, connector type, and group
 * token.
 *
 * <p>Shape differences vs v1.6:
 * <ul>
 *   <li>{@code idTag} → {@link IdToken} object (carries {@code type} and optional
 *       {@code additionalInfo}).
 *   <li>{@code connectorId} (int) → optional {@code evseId} + optional
 *       {@link ConnectorType}; reserving the whole station is allowed when
 *       both are omitted and the station's {@code ReservationNonEvseSpecific}
 *       variable is true (H01.FR.19).
 *   <li>{@code parentIdTag} (string) → optional {@code groupIdToken}
 *       ({@link IdToken}).
 * </ul>
 *
 * <p>{@code id} and {@code expiryDateTime} are required (H01.FR.02 / schema).
 * {@code id} is scoped station-wide: re-sending the same {@code id} replaces
 * the prior reservation (H01.FR.02).
 */
public interface ReserveNowPort {

    CompletableFuture<ReserveNowResult> reserveNow(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int id,
            Instant expiryDateTime,
            IdToken idToken,
            Integer evseId,
            ConnectorType connectorType,
            IdToken groupIdToken);
}
