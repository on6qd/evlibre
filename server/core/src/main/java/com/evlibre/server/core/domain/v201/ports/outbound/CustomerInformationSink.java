package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;

/**
 * Fires once per completed {@code NotifyCustomerInformation} transmission
 * (the frame with {@code tbc=false}), delivering the fully concatenated
 * {@code data} string across all frames keyed on the same {@code requestId}.
 *
 * <p>The data is spec-free ("No format specified in which the data is
 * returned. Should be human readable.") — subscribers are responsible for any
 * parsing. Absence of any data frames (only one empty frame with tbc=false)
 * still fires with an empty string so subscribers can distinguish "the
 * station has no customer data matching the request" from "the station did
 * not respond at all".
 */
@FunctionalInterface
public interface CustomerInformationSink {

    void onCustomerInformation(TenantId tenantId,
                                ChargePointIdentity stationIdentity,
                                int requestId,
                                String data);
}
