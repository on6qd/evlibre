package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetInstalledCertificateIdsResult;
import com.evlibre.server.core.domain.v201.dto.GetInstalledCertificateIdsStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.GetInstalledCertificateIdsPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.security.CertificateHashDataChain;
import com.evlibre.server.core.domain.v201.security.GetCertificateIdUse;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GetInstalledCertificateIdsUseCaseV201 implements GetInstalledCertificateIdsPort {

    private static final Logger log = LoggerFactory.getLogger(GetInstalledCertificateIdsUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public GetInstalledCertificateIdsUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<GetInstalledCertificateIdsResult> getInstalledCertificateIds(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            List<GetCertificateIdUse> certificateType) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");

        Map<String, Object> payload = new LinkedHashMap<>();
        if (certificateType != null && !certificateType.isEmpty()) {
            List<String> wire = new ArrayList<>(certificateType.size());
            for (GetCertificateIdUse use : certificateType) {
                wire.add(SecurityWire.getCertificateIdUseToWire(use));
            }
            payload.put("certificateType", wire);
        }

        log.info("Sending GetInstalledCertificateIds(types={}) to {} (tenant: {})",
                certificateType, stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "GetInstalledCertificateIds", payload)
                .thenApply(response -> parseResponse(stationIdentity, response));
    }

    private static GetInstalledCertificateIdsResult parseResponse(
            ChargePointIdentity stationIdentity, Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        GetInstalledCertificateIdsStatus status = switch (statusWire) {
            case "Accepted" -> GetInstalledCertificateIdsStatus.ACCEPTED;
            case "NotFound" -> GetInstalledCertificateIdsStatus.NOT_FOUND;
            default -> throw new IllegalStateException(
                    "Unexpected GetInstalledCertificateIds status from station: " + statusWire);
        };

        List<CertificateHashDataChain> chains = new ArrayList<>();
        Object chainsRaw = response.get("certificateHashDataChain");
        if (chainsRaw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> m) {
                    chains.add(chainFromWire(m));
                }
            }
        }

        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }

        log.info("GetInstalledCertificateIds response from {}: {} ({} chains)",
                stationIdentity.value(), statusWire, chains.size());

        return new GetInstalledCertificateIdsResult(status, chains, statusInfoReason);
    }

    private static CertificateHashDataChain chainFromWire(Map<?, ?> m) {
        GetCertificateIdUse type = SecurityWire.getCertificateIdUseFromWire(
                String.valueOf(m.get("certificateType")));
        Map<?, ?> leaf = (Map<?, ?>) m.get("certificateHashData");
        List<com.evlibre.server.core.domain.v201.security.CertificateHashData> children = new ArrayList<>();
        Object childrenRaw = m.get("childCertificateHashData");
        if (childrenRaw instanceof List<?> list) {
            for (Object child : list) {
                if (child instanceof Map<?, ?> cm) {
                    children.add(SecurityWire.certificateHashDataFromWire(cm));
                }
            }
        }
        return new CertificateHashDataChain(
                type, SecurityWire.certificateHashDataFromWire(leaf), children);
    }
}
