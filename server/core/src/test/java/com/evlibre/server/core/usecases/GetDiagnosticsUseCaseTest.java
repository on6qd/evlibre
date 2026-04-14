package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetDiagnosticsUseCaseTest {

    private StubCommandSender commandSender;
    private GetDiagnosticsUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new GetDiagnosticsUseCase(commandSender);
    }

    @Test
    void sends_location_and_optional_params() {
        commandSender.setNextResponse(Map.of("fileName", "diag-2025.txt"));

        CommandResult result = useCase.getDiagnostics(tenantId, station,
                "ftp://example.com/diag", 3, 60, null, null).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetDiagnostics");
        assertThat(cmd.payload()).containsEntry("location", "ftp://example.com/diag");
        assertThat(cmd.payload()).containsEntry("retries", 3);
        assertThat(cmd.payload()).containsEntry("retryInterval", 60);
        assertThat(cmd.payload()).doesNotContainKey("startTime");
    }

    @Test
    void empty_fileName_returns_noFile() {
        commandSender.setNextResponse(Map.of());

        CommandResult result = useCase.getDiagnostics(tenantId, station,
                "ftp://example.com/diag", null, null, null, null).join();

        assertThat(result.status()).isEqualTo("NoFile");
    }
}
