package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleDataTransferPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class HandleDataTransferUseCaseV201 implements HandleDataTransferPort {

    private static final Logger log = LoggerFactory.getLogger(HandleDataTransferUseCaseV201.class);

    private final OcppEventLogPort eventLog;
    // Case-insensitive allow-list of vendorIds the CSMS recognizes. OCPP 2.0.1 P01.FR.02
    // recommends the reverse-DNS form (e.g. "com.vendor.feature"). Empty by default —
    // every incoming DataTransfer is answered with UnknownVendorId unless the operator
    // adds an entry here.
    private final Set<String> knownVendorIds;

    public HandleDataTransferUseCaseV201(OcppEventLogPort eventLog) {
        this(eventLog, Collections.emptySet());
    }

    public HandleDataTransferUseCaseV201(OcppEventLogPort eventLog, Set<String> knownVendorIds) {
        this.eventLog = Objects.requireNonNull(eventLog);
        this.knownVendorIds = Objects.requireNonNull(knownVendorIds).stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public DataTransferResult handleDataTransfer(TenantId tenantId, String vendorId, String messageId, Object data) {
        log.info("DataTransfer received (tenant: {}, vendor: {}, messageId: {})",
                tenantId.value(), vendorId, messageId);
        eventLog.logEvent(tenantId.value(), "DataTransfer", "DataTransfer", "CS->CSMS",
                String.format("vendorId=%s, messageId=%s", vendorId, messageId));

        // OCPP 2.0.1 P02.FR.06: unknown vendorId MUST be answered with UnknownVendorId.
        String vendorKey = vendorId == null ? "" : vendorId.toLowerCase(Locale.ROOT);
        if (!knownVendorIds.contains(vendorKey)) {
            return DataTransferResult.of(DataTransferStatus.UNKNOWN_VENDOR_ID);
        }

        // Known vendor but no registered messageId handler → UnknownMessageId (P02.FR.07).
        // This CSMS has no vendor handlers yet; any messageId under a known vendor
        // currently falls through to Accepted (ignore).
        return DataTransferResult.of(DataTransferStatus.ACCEPTED);
    }
}
