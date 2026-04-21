package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.SetChargingProfilePort;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetChargingProfileUseCase implements SetChargingProfilePort {

    private static final Logger log = LoggerFactory.getLogger(SetChargingProfileUseCase.class);
    private final Ocpp16StationCommandSender commandSender;

    public SetChargingProfileUseCase(Ocpp16StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> setChargingProfile(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                                 int connectorId, Map<String, Object> csChargingProfiles) {
        // OCPP 1.6 §5.16 / §3.13: connectorId-to-purpose rules.
        //   ChargePointMaxProfile  → connectorId MUST be 0 (station-wide limit)
        //   TxDefaultProfile       → any connectorId (0 = all; >0 = specific)
        //   TxProfile              → connectorId MUST be > 0 (binds to a running tx)
        if (csChargingProfiles != null) {
            Object purpose = csChargingProfiles.get("chargingProfilePurpose");
            if ("ChargePointMaxProfile".equals(purpose) && connectorId != 0) {
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "ChargePointMaxProfile requires connectorId=0, was " + connectorId));
            }
            if ("TxProfile".equals(purpose) && connectorId <= 0) {
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "TxProfile requires connectorId>0, was " + connectorId));
            }
        }

        log.info("Sending SetChargingProfile to {} (tenant: {})", stationIdentity.value(), tenantId.value());

        Map<String, Object> payload = Map.of(
                "connectorId", connectorId,
                "csChargingProfiles", csChargingProfiles
        );

        return commandSender.sendCommand(tenantId, stationIdentity, "SetChargingProfile", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("SetChargingProfile response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }
}
