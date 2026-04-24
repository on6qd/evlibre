package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationResult;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationStatus;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationTarget;
import com.evlibre.server.core.domain.v201.ports.inbound.CustomerInformationPort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.evlibre.server.core.domain.v201.security.SecurityWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CustomerInformationUseCaseV201 implements CustomerInformationPort {

    private static final Logger log = LoggerFactory.getLogger(CustomerInformationUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public CustomerInformationUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CustomerInformationResult> customerInformation(TenantId tenantId,
                                                                              ChargePointIdentity stationIdentity,
                                                                              int requestId,
                                                                              boolean report,
                                                                              boolean clear,
                                                                              CustomerInformationTarget target) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(target, "target must not be null (use CustomerInformationTarget.none())");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("report", report);
        payload.put("clear", clear);
        if (target.customerIdentifier() != null) {
            payload.put("customerIdentifier", target.customerIdentifier());
        }
        if (target.idToken() != null) {
            payload.put("idToken", IdTokenWire.toWire(target.idToken()));
        }
        if (target.certificate() != null) {
            payload.put("customerCertificate", SecurityWire.certificateHashDataToWire(target.certificate()));
        }

        log.info("Sending CustomerInformation(requestId={}, report={}, clear={}, target={}) to {} (tenant: {})",
                requestId, report, clear, describeTarget(target),
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "CustomerInformation", payload)
                .thenApply(CustomerInformationUseCaseV201::decodeResponse);
    }

    private static CustomerInformationResult decodeResponse(Map<String, Object> response) {
        CustomerInformationStatus status = statusFromWire((String) response.get("status"));
        String statusInfoReason = null;
        Object statusInfo = response.get("statusInfo");
        if (statusInfo instanceof Map<?, ?> siMap) {
            Object reason = siMap.get("reasonCode");
            if (reason instanceof String s) {
                statusInfoReason = s;
            }
        }
        return new CustomerInformationResult(status, statusInfoReason);
    }

    private static CustomerInformationStatus statusFromWire(String wire) {
        return switch (wire) {
            case "Accepted" -> CustomerInformationStatus.ACCEPTED;
            case "Rejected" -> CustomerInformationStatus.REJECTED;
            case "Invalid" -> CustomerInformationStatus.INVALID;
            default -> throw new IllegalArgumentException(
                    "Unknown CustomerInformationStatus wire value: " + wire);
        };
    }

    private static String describeTarget(CustomerInformationTarget t) {
        if (t.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        if (t.customerIdentifier() != null) sb.append("identifier ");
        if (t.idToken() != null) sb.append("idToken ");
        if (t.certificate() != null) sb.append("certificate ");
        return sb.toString().trim();
    }
}
