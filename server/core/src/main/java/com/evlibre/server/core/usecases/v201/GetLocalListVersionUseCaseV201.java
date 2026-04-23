package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetLocalListVersionResult;
import com.evlibre.server.core.domain.v201.ports.inbound.GetLocalListVersionPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetLocalListVersionUseCaseV201 implements GetLocalListVersionPort {

    private static final Logger log = LoggerFactory.getLogger(GetLocalListVersionUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetLocalListVersionUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetLocalListVersionResult> getLocalListVersion(
            TenantId tenantId,
            ChargePointIdentity stationIdentity) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");

        log.info("Sending GetLocalListVersion to {} (tenant: {})",
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetLocalListVersion", Map.of())
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static GetLocalListVersionResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        Object raw = response.get("versionNumber");
        if (!(raw instanceof Number n)) {
            throw new IllegalStateException(
                    "GetLocalListVersion response from " + stationIdentity.value()
                            + " missing or non-numeric versionNumber: " + raw);
        }
        int versionNumber = n.intValue();
        log.info("GetLocalListVersion response from {}: versionNumber={}",
                stationIdentity.value(), versionNumber);
        return new GetLocalListVersionResult(versionNumber, response);
    }
}
