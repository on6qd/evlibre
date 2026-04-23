package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.ReserveNowResult;
import com.evlibre.server.core.domain.v201.dto.ReserveNowStatus;
import com.evlibre.server.core.domain.v201.model.ConnectorType;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReserveNowUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ReserveNowUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");
    private final Instant expiry = Instant.parse("2027-01-01T12:00:00Z");
    private final IdToken driver = IdToken.of("RFID-DRIVER-01", IdTokenType.ISO14443);

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ReserveNowUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void payload_carries_required_fields_only() {
        useCase.reserveNow(tenantId, station, 42, expiry, driver, null, null, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ReserveNow");
        assertThat(cmd.payload())
                .containsEntry("id", 42)
                .containsEntry("expiryDateTime", "2027-01-01T12:00:00Z")
                .doesNotContainKeys("evseId", "connectorType", "groupIdToken");

        @SuppressWarnings("unchecked")
        Map<String, Object> idTokenWire = (Map<String, Object>) cmd.payload().get("idToken");
        assertThat(idTokenWire)
                .containsEntry("idToken", "RFID-DRIVER-01")
                .containsEntry("type", "ISO14443");
    }

    @Test
    void payload_includes_optional_evse_connector_type_and_group_id() {
        IdToken fleet = IdToken.of("FLEET-ACME", IdTokenType.CENTRAL);

        useCase.reserveNow(tenantId, station, 7, expiry, driver, 2, ConnectorType.CCCS2, fleet).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload())
                .containsEntry("evseId", 2)
                .containsEntry("connectorType", "cCCS2");

        @SuppressWarnings("unchecked")
        Map<String, Object> groupWire = (Map<String, Object>) cmd.payload().get("groupIdToken");
        assertThat(groupWire)
                .containsEntry("idToken", "FLEET-ACME")
                .containsEntry("type", "Central");
    }

    @Test
    void accepted_status_parsed() {
        ReserveNowResult r = useCase.reserveNow(tenantId, station, 1, expiry, driver, null, null, null).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(ReserveNowStatus.ACCEPTED);
    }

    @Test
    void occupied_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Occupied"));

        ReserveNowResult r = useCase.reserveNow(tenantId, station, 1, expiry, driver, 1, null, null).join();

        assertThat(r.status()).isEqualTo(ReserveNowStatus.OCCUPIED);
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void faulted_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Faulted"));

        ReserveNowResult r = useCase.reserveNow(tenantId, station, 1, expiry, driver, null, null, null).join();

        assertThat(r.status()).isEqualTo(ReserveNowStatus.FAULTED);
    }

    @Test
    void unavailable_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "Unavailable"));

        ReserveNowResult r = useCase.reserveNow(tenantId, station, 1, expiry, driver, null, null, null).join();

        assertThat(r.status()).isEqualTo(ReserveNowStatus.UNAVAILABLE);
    }

    @Test
    void rejected_status_surfaces_status_info_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "ReservationsNotSupported")));

        ReserveNowResult r = useCase.reserveNow(tenantId, station, 1, expiry, driver, null, null, null).join();

        assertThat(r.status()).isEqualTo(ReserveNowStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("ReservationsNotSupported");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() -> useCase.reserveNow(tenantId, station, 1, expiry, driver, null, null, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void non_positive_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.reserveNow(tenantId, station, 1, expiry, driver, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
        assertThatThrownBy(() -> useCase.reserveNow(tenantId, station, 1, expiry, driver, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void null_expiry_rejected() {
        assertThatThrownBy(() -> useCase.reserveNow(tenantId, station, 1, null, driver, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("expiryDateTime");
    }

    @Test
    void null_id_token_rejected() {
        assertThatThrownBy(() -> useCase.reserveNow(tenantId, station, 1, expiry, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idToken");
    }
}
