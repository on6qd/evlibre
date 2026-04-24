package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.InstallCertificateResult;
import com.evlibre.server.core.domain.v201.dto.InstallCertificateStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.InstallCertificatePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.security.InstallCertificateUse;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class InstallCertificateUseCaseV201 implements InstallCertificatePort {

    private static final Logger log = LoggerFactory.getLogger(InstallCertificateUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public InstallCertificateUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<InstallCertificateResult> installCertificate(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            InstallCertificateUse certificateType,
            String certificate) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(certificateType, "certificateType");
        Objects.requireNonNull(certificate, "certificate");
        if (certificate.isBlank()) {
            throw new IllegalArgumentException("certificate must not be blank");
        }
        if (certificate.length() > 5500) {
            throw new IllegalArgumentException(
                    "certificate exceeds 5500 char limit (" + certificate.length() + ")");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("certificateType", SecurityWire.installCertificateUseToWire(certificateType));
        payload.put("certificate", certificate);

        log.info("Sending InstallCertificate(type={}, pemLength={}) to {} (tenant: {})",
                certificateType, certificate.length(), stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "InstallCertificate", payload)
                .thenApply(response -> parseResponse(stationIdentity, certificateType, response));
    }

    private static InstallCertificateResult parseResponse(
            ChargePointIdentity stationIdentity,
            InstallCertificateUse type,
            Map<String, Object> response) {
        String statusWire = String.valueOf(response.getOrDefault("status", ""));
        InstallCertificateStatus status = switch (statusWire) {
            case "Accepted" -> InstallCertificateStatus.ACCEPTED;
            case "Rejected" -> InstallCertificateStatus.REJECTED;
            case "Failed" -> InstallCertificateStatus.FAILED;
            default -> throw new IllegalStateException(
                    "Unexpected InstallCertificate status from station: " + statusWire);
        };
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> m) {
            Object reason = m.get("reasonCode");
            statusInfoReason = reason == null ? null : String.valueOf(reason);
        }
        log.info("InstallCertificate response from {} (type={}): {}",
                stationIdentity.value(), type, statusWire);
        return new InstallCertificateResult(status, statusInfoReason);
    }
}
