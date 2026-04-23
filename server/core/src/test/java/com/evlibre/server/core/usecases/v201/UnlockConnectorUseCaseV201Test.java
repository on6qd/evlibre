package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.UnlockConnectorResult;
import com.evlibre.server.core.domain.v201.dto.UnlockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnlockConnectorUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private UnlockConnectorUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new UnlockConnectorUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Unlocked"));
    }

    @Test
    void payload_carries_evse_id_and_connector_id() {
        useCase.unlockConnector(tenantId, station, 2, 1).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("UnlockConnector");
        assertThat(cmd.payload())
                .containsEntry("evseId", 2)
                .containsEntry("connectorId", 1);
    }

    @Test
    void unlocked_status_parsed() {
        UnlockConnectorResult r = useCase.unlockConnector(tenantId, station, 1, 1).join();

        assertThat(r.isUnlocked()).isTrue();
        assertThat(r.status()).isEqualTo(UnlockStatus.UNLOCKED);
    }

    @Test
    void unlock_failed_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "UnlockFailed"));

        UnlockConnectorResult r = useCase.unlockConnector(tenantId, station, 1, 1).join();

        assertThat(r.status()).isEqualTo(UnlockStatus.UNLOCK_FAILED);
        assertThat(r.isUnlocked()).isFalse();
    }

    @Test
    void ongoing_authorized_transaction_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "OngoingAuthorizedTransaction"));

        UnlockConnectorResult r = useCase.unlockConnector(tenantId, station, 1, 1).join();

        assertThat(r.status()).isEqualTo(UnlockStatus.ONGOING_AUTHORIZED_TRANSACTION);
    }

    @Test
    void unknown_connector_surfaces_status_info_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "UnknownConnector",
                "statusInfo", Map.of("reasonCode", "NoSuchConnector")));

        UnlockConnectorResult r = useCase.unlockConnector(tenantId, station, 9, 9).join();

        assertThat(r.status()).isEqualTo(UnlockStatus.UNKNOWN_CONNECTOR);
        assertThat(r.statusInfoReason()).isEqualTo("NoSuchConnector");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.unlockConnector(tenantId, station, 1, 1).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void non_positive_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.unlockConnector(tenantId, station, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
        assertThatThrownBy(() -> useCase.unlockConnector(tenantId, station, -1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void non_positive_connector_id_rejected() {
        assertThatThrownBy(() -> useCase.unlockConnector(tenantId, station, 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
        assertThatThrownBy(() -> useCase.unlockConnector(tenantId, station, 1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
    }
}
