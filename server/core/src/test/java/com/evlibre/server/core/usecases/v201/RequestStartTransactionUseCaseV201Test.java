package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.RequestStartStopStatus;
import com.evlibre.server.core.domain.v201.dto.RequestStartTransactionResult;
import com.evlibre.server.core.domain.v201.model.AdditionalInfo;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestStartTransactionUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private RequestStartTransactionUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new RequestStartTransactionUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void minimal_payload_carries_remote_start_id_and_id_token() {
        useCase.requestStartTransaction(tenantId, station, 77,
                IdToken.of("DRIVER-42", IdTokenType.ISO14443), null, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("RequestStartTransaction");
        assertThat(cmd.payload())
                .containsEntry("remoteStartId", 77)
                .doesNotContainKey("evseId")
                .doesNotContainKey("groupIdToken");

        @SuppressWarnings("unchecked")
        Map<String, Object> idTokenWire = (Map<String, Object>) cmd.payload().get("idToken");
        assertThat(idTokenWire)
                .containsEntry("idToken", "DRIVER-42")
                .containsEntry("type", "ISO14443")
                .doesNotContainKey("additionalInfo");
    }

    @Test
    void evse_id_is_propagated_when_present() {
        useCase.requestStartTransaction(tenantId, station, 1,
                IdToken.of("x", IdTokenType.CENTRAL), 3, null).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("evseId", 3);
    }

    @Test
    void group_id_token_serialises_as_nested_object() {
        useCase.requestStartTransaction(tenantId, station, 1,
                IdToken.of("driver", IdTokenType.EMAID), null,
                IdToken.of("fleet-a", IdTokenType.CENTRAL)).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> group = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("groupIdToken");
        assertThat(group)
                .containsEntry("idToken", "fleet-a")
                .containsEntry("type", "Central");
    }

    @Test
    void additional_info_array_serialises_with_required_fields() {
        IdToken token = new IdToken("driver", IdTokenType.ISO14443,
                List.of(new AdditionalInfo("parent-1", "parent"),
                        new AdditionalInfo("alt-2", "alias")));

        useCase.requestStartTransaction(tenantId, station, 1, token, null, null).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> idTokenWire = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("idToken");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extras = (List<Map<String, Object>>) idTokenWire.get("additionalInfo");
        assertThat(extras).hasSize(2);
        assertThat(extras.get(0))
                .containsEntry("additionalIdToken", "parent-1")
                .containsEntry("type", "parent");
    }

    @Test
    void all_id_token_types_map_to_exact_wire_strings() {
        for (IdTokenType type : IdTokenType.values()) {
            commandSender = new StubCommandSender201();
            commandSender.setNextResponse(Map.of("status", "Accepted"));
            useCase = new RequestStartTransactionUseCaseV201(commandSender);

            useCase.requestStartTransaction(tenantId, station, 1,
                    IdToken.of("x", type), null, null).join();

            @SuppressWarnings("unchecked")
            Map<String, Object> idTokenWire = (Map<String, Object>) commandSender.commands().get(0)
                    .payload().get("idToken");
            assertThat(idTokenWire.get("type"))
                    .as("wire value for %s", type)
                    .isIn("Central", "eMAID", "ISO14443", "ISO15693",
                            "KeyCode", "Local", "MacAddress", "NoAuthorization");
        }
    }

    @Test
    void accepted_with_transaction_id_echo_parsed() {
        commandSender.setNextResponse(Map.of(
                "status", "Accepted",
                "transactionId", "tx-abc-123"));

        RequestStartTransactionResult result = useCase.requestStartTransaction(
                tenantId, station, 1, IdToken.of("x", IdTokenType.CENTRAL), null, null).join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.status()).isEqualTo(RequestStartStopStatus.ACCEPTED);
        assertThat(result.transactionId()).isEqualTo("tx-abc-123");
    }

    @Test
    void rejected_surfaces_status_info_reason_code() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "UnknownEvse")));

        RequestStartTransactionResult result = useCase.requestStartTransaction(
                tenantId, station, 1, IdToken.of("x", IdTokenType.CENTRAL), 99, null).join();

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.status()).isEqualTo(RequestStartStopStatus.REJECTED);
        assertThat(result.statusInfoReason()).isEqualTo("UnknownEvse");
    }

    @Test
    void unknown_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Pending"));

        assertThatThrownBy(() -> useCase.requestStartTransaction(
                tenantId, station, 1, IdToken.of("x", IdTokenType.CENTRAL), null, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pending");
    }

    @Test
    void null_id_token_rejected() {
        assertThatThrownBy(() -> useCase.requestStartTransaction(
                tenantId, station, 1, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idToken");
    }

    @Test
    void non_positive_evse_id_rejected() {
        assertThatThrownBy(() -> useCase.requestStartTransaction(
                tenantId, station, 1, IdToken.of("x", IdTokenType.CENTRAL), 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
        assertThatThrownBy(() -> useCase.requestStartTransaction(
                tenantId, station, 1, IdToken.of("x", IdTokenType.CENTRAL), -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }
}
