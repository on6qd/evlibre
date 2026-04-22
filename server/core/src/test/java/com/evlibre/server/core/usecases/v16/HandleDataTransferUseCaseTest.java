package com.evlibre.server.core.usecases.v16;

import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HandleDataTransferUseCaseTest {

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final OcppEventLogPort noopLog = (s, m, a, d, p) -> {};

    // OCPP 1.6 §5.13: unknown vendorId → UnknownVendorId.
    @Test
    void unknown_vendor_returns_unknownVendorId() {
        var useCase = new HandleDataTransferUseCase(noopLog);

        CommandResult result = useCase.handleDataTransfer(tenantId, "com.unknown.vendor", "ping", null);

        assertThat(result.status()).isEqualTo("UnknownVendorId");
    }

    @Test
    void known_vendor_returns_accepted() {
        var useCase = new HandleDataTransferUseCase(noopLog, Set.of("com.evlibre.probe"));

        CommandResult result = useCase.handleDataTransfer(tenantId, "com.evlibre.probe", "ping", null);

        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void vendor_match_is_case_insensitive() {
        var useCase = new HandleDataTransferUseCase(noopLog, Set.of("com.evlibre.probe"));

        CommandResult result = useCase.handleDataTransfer(tenantId, "COM.EVLIBRE.PROBE", "ping", null);

        assertThat(result.isAccepted()).isTrue();
    }
}
