package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartTransactionResult;
import com.evlibre.server.core.domain.v201.model.IdToken;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code RequestStartTransaction} for OCPP 2.0.1 (use case F01).
 *
 * <p>The CSMS asks the station to start a transaction on behalf of an EV
 * driver. {@code remoteStartId} is the CSMS-chosen correlation id; the station
 * will echo it on the follow-up {@code TransactionEvent(Started)} so the CSMS
 * can reconcile.
 *
 * <p>Separate from the v1.6 {@code RemoteStartTransaction} — the v2.0.1 request
 * carries an {@link IdToken} object instead of an {@code idTag} string, adds
 * the optional {@code evseId}/{@code groupIdToken}, and the response may echo
 * a {@code transactionId} when the station was already mid-transaction.
 *
 * @param evseId absent = station picks the EVSE; present = start on that
 *               EVSE specifically. Spec constrains {@code evseId > 0} when set.
 * @param groupIdToken absent = no group; present = carries the parent/group
 *               identity (e.g. for fleet or tariff grouping).
 */
public interface RequestStartTransactionPort {

    CompletableFuture<RequestStartTransactionResult> requestStartTransaction(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int remoteStartId,
            IdToken idToken,
            Integer evseId,
            IdToken groupIdToken);
}
