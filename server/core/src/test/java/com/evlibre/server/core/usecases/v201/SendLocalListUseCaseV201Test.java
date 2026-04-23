package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.SendLocalListResult;
import com.evlibre.server.core.domain.v201.dto.SendLocalListStatus;
import com.evlibre.server.core.domain.v201.model.AuthorizationData;
import com.evlibre.server.core.domain.v201.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenInfo;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.domain.v201.model.UpdateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendLocalListUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SendLocalListUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SendLocalListUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void full_update_with_empty_list_omits_list_field() {
        useCase.sendLocalList(tenantId, station, 7, UpdateType.FULL, List.of()).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("SendLocalList");
        assertThat(cmd.payload())
                .containsEntry("versionNumber", 7)
                .containsEntry("updateType", "Full")
                .doesNotContainKey("localAuthorizationList");
    }

    @Test
    void differential_add_entry_wires_full_id_token_info() {
        var entry = AuthorizationData.add(
                IdToken.of("driver-42", IdTokenType.ISO14443),
                new IdTokenInfo(
                        AuthorizationStatus.ACCEPTED,
                        Instant.parse("2030-01-01T00:00:00Z"),
                        5,
                        List.of(1, 2),
                        IdToken.of("parent-group", IdTokenType.CENTRAL),
                        "en",
                        "nl",
                        null));

        useCase.sendLocalList(tenantId, station, 12, UpdateType.DIFFERENTIAL, List.of(entry)).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("updateType", "Differential");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) cmd.payload().get("localAuthorizationList");
        assertThat(list).hasSize(1);
        Map<String, Object> wired = list.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> idToken = (Map<String, Object>) wired.get("idToken");
        assertThat(idToken).containsEntry("idToken", "driver-42").containsEntry("type", "ISO14443");
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) wired.get("idTokenInfo");
        assertThat(info)
                .containsEntry("status", "Accepted")
                .containsEntry("cacheExpiryDateTime", "2030-01-01T00:00:00Z")
                .containsEntry("chargingPriority", 5)
                .containsEntry("evseId", List.of(1, 2))
                .containsEntry("language1", "en")
                .containsEntry("language2", "nl");
        @SuppressWarnings("unchecked")
        Map<String, Object> group = (Map<String, Object>) info.get("groupIdToken");
        assertThat(group).containsEntry("idToken", "parent-group").containsEntry("type", "Central");
    }

    @Test
    void differential_remove_entry_omits_id_token_info() {
        var entry = AuthorizationData.remove(IdToken.of("driver-42", IdTokenType.ISO14443));

        useCase.sendLocalList(tenantId, station, 12, UpdateType.DIFFERENTIAL, List.of(entry)).join();

        var cmd = commandSender.commands().get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) cmd.payload().get("localAuthorizationList");
        Map<String, Object> wired = list.get(0);
        assertThat(wired).containsKey("idToken").doesNotContainKey("idTokenInfo");
    }

    @Test
    void idToken_type_enum_values_map_to_exact_wire_spelling() {
        // The eMAID spelling matters — it's a known quirk of the v2.0.1 spec.
        useCase.sendLocalList(tenantId, station, 1, UpdateType.FULL,
                List.of(AuthorizationData.remove(IdToken.of("x", IdTokenType.EMAID)))).join();

        var cmd = commandSender.commands().get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) cmd.payload().get("localAuthorizationList");
        @SuppressWarnings("unchecked")
        Map<String, Object> idToken = (Map<String, Object>) list.get(0).get("idToken");
        assertThat(idToken).containsEntry("type", "eMAID");
    }

    @Test
    void auth_status_enum_values_map_to_exact_wire_spelling() {
        // Cover the values with non-obvious capitalisation.
        var entries = List.of(
                AuthorizationData.add(IdToken.of("a", IdTokenType.CENTRAL), IdTokenInfo.of(AuthorizationStatus.CONCURRENT_TX)),
                AuthorizationData.add(IdToken.of("b", IdTokenType.CENTRAL), IdTokenInfo.of(AuthorizationStatus.NOT_ALLOWED_TYPE_EVSE)),
                AuthorizationData.add(IdToken.of("c", IdTokenType.CENTRAL), IdTokenInfo.of(AuthorizationStatus.NOT_AT_THIS_LOCATION)),
                AuthorizationData.add(IdToken.of("d", IdTokenType.CENTRAL), IdTokenInfo.of(AuthorizationStatus.NOT_AT_THIS_TIME)));

        useCase.sendLocalList(tenantId, station, 1, UpdateType.FULL, entries).join();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wired = (List<Map<String, Object>>)
                commandSender.commands().get(0).payload().get("localAuthorizationList");
        List<String> statuses = wired.stream()
                .map(entry -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> info = (Map<String, Object>) entry.get("idTokenInfo");
                    return (String) info.get("status");
                })
                .toList();
        assertThat(statuses).containsExactly(
                "ConcurrentTx", "NotAllowedTypeEVSE", "NotAtThisLocation", "NotAtThisTime");
    }

    @Test
    void accepted_response_parsed() {
        SendLocalListResult r = useCase
                .sendLocalList(tenantId, station, 1, UpdateType.FULL, List.of()).join();

        assertThat(r.status()).isEqualTo(SendLocalListStatus.ACCEPTED);
        assertThat(r.isAccepted()).isTrue();
    }

    @Test
    void failed_response_parsed() {
        commandSender.setNextResponse(Map.of("status", "Failed"));

        SendLocalListResult r = useCase
                .sendLocalList(tenantId, station, 1, UpdateType.FULL, List.of()).join();

        assertThat(r.status()).isEqualTo(SendLocalListStatus.FAILED);
    }

    @Test
    void version_mismatch_parsed_with_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "VersionMismatch",
                "statusInfo", Map.of("reasonCode", "OutOfSequence")));

        SendLocalListResult r = useCase
                .sendLocalList(tenantId, station, 2, UpdateType.DIFFERENTIAL, List.of()).join();

        assertThat(r.status()).isEqualTo(SendLocalListStatus.VERSION_MISMATCH);
        assertThat(r.statusInfoReason()).isEqualTo("OutOfSequence");
    }

    @Test
    void non_positive_version_rejected() {
        assertThatThrownBy(() ->
                useCase.sendLocalList(tenantId, station, 0, UpdateType.FULL, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versionNumber");
        assertThatThrownBy(() ->
                useCase.sendLocalList(tenantId, station, -1, UpdateType.FULL, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versionNumber");
    }

    @Test
    void null_update_type_rejected() {
        assertThatThrownBy(() ->
                useCase.sendLocalList(tenantId, station, 1, null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("updateType");
    }

    @Test
    void null_list_rejected() {
        assertThatThrownBy(() ->
                useCase.sendLocalList(tenantId, station, 1, UpdateType.FULL, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("localAuthorizationList");
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() ->
                useCase.sendLocalList(tenantId, station, 1, UpdateType.FULL, List.of()).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }
}
