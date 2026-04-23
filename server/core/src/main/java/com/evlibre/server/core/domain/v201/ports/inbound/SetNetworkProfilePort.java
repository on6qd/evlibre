package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.network.NetworkConnectionProfile;

import java.util.concurrent.CompletableFuture;

/**
 * CSMS-initiated {@code SetNetworkProfile} (OCPP 2.0.1 B09 — Setting a new
 * Network Connection Profile).
 *
 * <p>Synchronous acknowledgement, then delayed activation: on {@code Accepted}
 * the station has stored the profile at the given {@code configurationSlot}
 * but will only switch to it after the next reboot. The CSMS is responsible
 * for sequencing the usual follow-up steps ({@code SetVariables} on
 * {@code NetworkConfigurationPriority}, then {@code Reset}).
 */
public interface SetNetworkProfilePort {

    CompletableFuture<CommandResult> setNetworkProfile(TenantId tenantId,
                                                        ChargePointIdentity stationIdentity,
                                                        int configurationSlot,
                                                        NetworkConnectionProfile profile);
}
