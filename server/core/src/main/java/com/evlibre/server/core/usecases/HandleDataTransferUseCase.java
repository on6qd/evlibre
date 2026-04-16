package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.HandleDataTransferPort;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class HandleDataTransferUseCase implements HandleDataTransferPort {

    private static final Logger log = LoggerFactory.getLogger(HandleDataTransferUseCase.class);

    private final OcppEventLogPort eventLog;
    // Case-insensitive allow-list of vendorIds the CSMS recognizes. Reverse-DNS form
    // per OCPP 1.6 §5.13 (e.g. "com.vendor.product"). Empty by default — all incoming
    // DataTransfer requests will be answered with UnknownVendor unless the operator
    // adds an entry here.
    private final Set<String> knownVendorIds;

    public HandleDataTransferUseCase(OcppEventLogPort eventLog) {
        this(eventLog, Collections.emptySet());
    }

    public HandleDataTransferUseCase(OcppEventLogPort eventLog, Set<String> knownVendorIds) {
        this.eventLog = Objects.requireNonNull(eventLog);
        this.knownVendorIds = Objects.requireNonNull(knownVendorIds).stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public CommandResult handleDataTransfer(TenantId tenantId, String vendorId, String messageId, String data) {
        log.info("DataTransfer received (tenant: {}, vendor: {}, messageId: {})",
                tenantId.value(), vendorId, messageId);
        eventLog.logEvent(tenantId.value(), "DataTransfer", "DataTransfer", "CS->CSMS",
                String.format("vendorId=%s, messageId=%s", vendorId, messageId));

        // OCPP 1.6 §5.13: unknown vendorId MUST be answered with UnknownVendor.
        String vendorKey = vendorId == null ? "" : vendorId.toLowerCase(Locale.ROOT);
        if (!knownVendorIds.contains(vendorKey)) {
            return new CommandResult("UnknownVendor");
        }

        // Known vendor but no registered messageId handler → UnknownMessageId.
        // This CSMS has no vendor handlers yet; any messageId under a known vendor
        // currently falls through to Accepted (ignore).
        return new CommandResult("Accepted");
    }
}
