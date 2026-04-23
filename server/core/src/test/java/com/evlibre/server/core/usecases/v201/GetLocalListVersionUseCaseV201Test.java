package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GetLocalListVersionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetLocalListVersionUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private GetLocalListVersionUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new GetLocalListVersionUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("versionNumber", 42));
    }

    @Test
    void payload_is_empty() {
        useCase.getLocalListVersion(tenantId, station).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetLocalListVersion");
        assertThat(cmd.payload()).isEmpty();
    }

    @Test
    void positive_version_means_list_installed() {
        GetLocalListVersionResult r = useCase.getLocalListVersion(tenantId, station).join();

        assertThat(r.versionNumber()).isEqualTo(42);
        assertThat(r.hasLocalList()).isTrue();
    }

    @Test
    void zero_version_means_no_list() {
        commandSender.setNextResponse(Map.of("versionNumber", 0));

        GetLocalListVersionResult r = useCase.getLocalListVersion(tenantId, station).join();

        assertThat(r.versionNumber()).isZero();
        assertThat(r.hasLocalList()).isFalse();
    }

    @Test
    void missing_version_rejected() {
        commandSender.setNextResponse(new HashMap<>());

        assertThatThrownBy(() -> useCase.getLocalListVersion(tenantId, station).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("versionNumber");
    }

    @Test
    void negative_version_rejected() {
        commandSender.setNextResponse(Map.of("versionNumber", -1));

        assertThatThrownBy(() -> useCase.getLocalListVersion(tenantId, station).join())
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versionNumber");
    }
}
