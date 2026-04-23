package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HandleDataTransferUseCaseV201Test {

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final OcppEventLogPort noopLog = (s, m, a, d, p) -> {};

    // OCPP 2.0.1 P02.FR.06: unknown vendorId → UnknownVendorId.
    @Test
    void unknown_vendor_returns_unknownVendorId() {
        var useCase = new HandleDataTransferUseCaseV201(noopLog);

        DataTransferResult result = useCase.handleDataTransfer(tenantId, "com.unknown.vendor", "ping", null);

        assertThat(result.status()).isEqualTo(DataTransferStatus.UNKNOWN_VENDOR_ID);
        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void known_vendor_returns_accepted() {
        var useCase = new HandleDataTransferUseCaseV201(noopLog, Set.of("com.evlibre.probe"));

        DataTransferResult result = useCase.handleDataTransfer(tenantId, "com.evlibre.probe", "ping", null);

        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void vendor_match_is_case_insensitive() {
        var useCase = new HandleDataTransferUseCaseV201(noopLog, Set.of("com.evlibre.probe"));

        DataTransferResult result = useCase.handleDataTransfer(tenantId, "COM.EVLIBRE.PROBE", "ping", null);

        assertThat(result.isAccepted()).isTrue();
    }

    // 2.0.1 `data` is anyType — domain port must accept arbitrary Java trees (maps, lists,
    // primitives) without choking. The handler converts Jackson JsonNode → plain tree.
    @Test
    void known_vendor_accepts_object_data() {
        var useCase = new HandleDataTransferUseCaseV201(noopLog, Set.of("com.evlibre.probe"));

        DataTransferResult result = useCase.handleDataTransfer(
                tenantId, "com.evlibre.probe", "start", Map.of("start_time", "2026-04-23T10:00:00Z"));

        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void null_vendor_rejected_as_unknown() {
        var useCase = new HandleDataTransferUseCaseV201(noopLog);

        DataTransferResult result = useCase.handleDataTransfer(tenantId, null, null, null);

        assertThat(result.status()).isEqualTo(DataTransferStatus.UNKNOWN_VENDOR_ID);
    }
}
