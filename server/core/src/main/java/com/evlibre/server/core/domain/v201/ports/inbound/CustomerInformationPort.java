package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationResult;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationTarget;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code CustomerInformation} (OCPP 2.0.1 O01 — Customer
 * Information). Used by operators to request or clear customer data held on
 * the Charging Station.
 *
 * <p>{@code report} and {@code clear} drive the intent: {@code report=true}
 * asks the station to stream matching data back via one or more
 * {@code NotifyCustomerInformation} calls; {@code clear=true} asks the
 * station to erase the matching data. Both may be set in the same request.
 */
public interface CustomerInformationPort {

    CompletableFuture<CustomerInformationResult> customerInformation(TenantId tenantId,
                                                                       ChargePointIdentity stationIdentity,
                                                                       int requestId,
                                                                       boolean report,
                                                                       boolean clear,
                                                                       CustomerInformationTarget target);
}
