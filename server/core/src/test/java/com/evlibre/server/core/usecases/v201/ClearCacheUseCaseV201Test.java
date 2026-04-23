package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ClearCacheResult;
import com.evlibre.server.core.domain.v201.dto.ClearCacheStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClearCacheUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ClearCacheUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ClearCacheUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void payload_is_empty() {
        useCase.clearCache(tenantId, station).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ClearCache");
        assertThat(cmd.payload()).isEmpty();
    }

    @Test
    void accepted_status_parsed() {
        ClearCacheResult r = useCase.clearCache(tenantId, station).join();

        assertThat(r.status()).isEqualTo(ClearCacheStatus.ACCEPTED);
        assertThat(r.isAccepted()).isTrue();
    }

    @Test
    void rejected_status_parsed_with_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "NotEnabled")));

        ClearCacheResult r = useCase.clearCache(tenantId, station).join();

        assertThat(r.status()).isEqualTo(ClearCacheStatus.REJECTED);
        assertThat(r.isAccepted()).isFalse();
        assertThat(r.statusInfoReason()).isEqualTo("NotEnabled");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.clearCache(tenantId, station).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }
}
