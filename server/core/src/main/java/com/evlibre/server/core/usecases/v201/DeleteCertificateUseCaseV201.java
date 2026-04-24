package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DeleteCertificateResult;
import com.evlibre.server.core.domain.v201.dto.DeleteCertificateStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.DeleteCertificatePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.security.CertificateHashData;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DeleteCertificateUseCaseV201 implements DeleteCertificatePort {

    private static final Logger log = LoggerFactory.getLogger(DeleteCertificateUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public DeleteCertificateUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<DeleteCertificateResult> deleteCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            CertificateHashData certificateHashData) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(certificateHashData, "certificateHashData");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("certificateHashData", SecurityWire.certificateHashDataToWire(certificateHashData));

        log.info("Sending DeleteCertificate(serial={}) to {} (tenant: {})",
                certificateHashData.serialNumber(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "DeleteCertificate", payload)
                .thenApply(response -> parseResponse(stationIdentity, certificateHashData, response));
    }

    private static DeleteCertificateResult parseResponse(
            ChargePointIdentity stationIdentity,
            CertificateHashData hash,
            Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        DeleteCertificateStatus status = switch (statusWire) {
            case "Accepted" -> DeleteCertificateStatus.ACCEPTED;
            case "Failed" -> DeleteCertificateStatus.FAILED;
            case "NotFound" -> DeleteCertificateStatus.NOT_FOUND;
            default -> throw new IllegalStateException(
                    "Unexpected DeleteCertificate status from station: " + statusWire);
        };
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("DeleteCertificate response from {} (serial={}): {}",
                stationIdentity.value(), hash.serialNumber(), statusWire);
        return new DeleteCertificateResult(status, statusInfoReason);
    }
}
