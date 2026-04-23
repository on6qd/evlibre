package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.network.ApnAuthMethod;
import com.evlibre.server.core.domain.v201.network.ApnConfig;
import com.evlibre.server.core.domain.v201.network.NetworkConnectionProfile;
import com.evlibre.server.core.domain.v201.network.OcppInterface;
import com.evlibre.server.core.domain.v201.network.OcppTransport;
import com.evlibre.server.core.domain.v201.network.OcppVersion;
import com.evlibre.server.core.domain.v201.network.VpnConfig;
import com.evlibre.server.core.domain.v201.network.VpnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetNetworkProfileUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SetNetworkProfileUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SetNetworkProfileUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void minimal_websocket_profile_sends_correct_payload() {
        var profile = NetworkConnectionProfile.ofWebSocket(
                OcppVersion.OCPP_20, OcppInterface.WIRED_0,
                "wss://csms.example.com/ocpp", 30, 3);

        CommandResult result = useCase.setNetworkProfile(tenantId, station, 1, profile).join();

        assertThat(result.isAccepted()).isTrue();
        assertThat(commandSender.commands()).hasSize(1);
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("SetNetworkProfile");
        assertThat(cmd.payload()).containsEntry("configurationSlot", 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>) cmd.payload().get("connectionData");
        assertThat(conn)
                .containsEntry("ocppVersion", "OCPP20")
                .containsEntry("ocppTransport", "JSON")
                .containsEntry("ocppInterface", "Wired0")
                .containsEntry("messageTimeout", 30)
                .containsEntry("securityProfile", 3)
                .containsEntry("ocppCsmsUrl", "wss://csms.example.com/ocpp")
                .doesNotContainKey("apn")
                .doesNotContainKey("vpn");
    }

    @Test
    void all_enum_values_map_to_spec_wire_form() {
        // OCPP version + interface cover the largest enums — one send per value.
        OcppVersion[] versions = OcppVersion.values();
        String[] expected = {"OCPP12", "OCPP15", "OCPP16", "OCPP20"};
        for (int i = 0; i < versions.length; i++) {
            var profile = new NetworkConnectionProfile(
                    versions[i], OcppTransport.JSON, OcppInterface.WIRED_0,
                    30, 1, "wss://x", null, null);
            useCase.setNetworkProfile(tenantId, station, 0, profile).join();
            @SuppressWarnings("unchecked")
            Map<String, Object> conn = (Map<String, Object>) commandSender.commands()
                    .get(commandSender.commands().size() - 1).payload().get("connectionData");
            assertThat(conn.get("ocppVersion")).isEqualTo(expected[i]);
        }

        commandSender = new StubCommandSender201();
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        useCase = new SetNetworkProfileUseCaseV201(commandSender);

        OcppInterface[] ifaces = OcppInterface.values();
        String[] expectedIf = {"Wired0", "Wired1", "Wired2", "Wired3",
                "Wireless0", "Wireless1", "Wireless2", "Wireless3"};
        for (int i = 0; i < ifaces.length; i++) {
            var profile = new NetworkConnectionProfile(
                    OcppVersion.OCPP_20, OcppTransport.JSON, ifaces[i],
                    30, 1, "wss://x", null, null);
            useCase.setNetworkProfile(tenantId, station, 0, profile).join();
        }
        for (int i = 0; i < ifaces.length; i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conn = (Map<String, Object>) commandSender.commands().get(i)
                    .payload().get("connectionData");
            assertThat(conn.get("ocppInterface")).isEqualTo(expectedIf[i]);
        }
    }

    @Test
    void vpn_type_ikev2_maps_to_pascal_spec_wire_form() {
        var vpn = new VpnConfig("vpn.example.com", "user", null, "pw", "secret", VpnType.IKE_V2);
        var profile = new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRELESS_0,
                30, 3, "wss://x", null, vpn);

        useCase.setNetworkProfile(tenantId, station, 0, profile).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("connectionData");
        @SuppressWarnings("unchecked")
        Map<String, Object> vpnWire = (Map<String, Object>) conn.get("vpn");
        assertThat(vpnWire)
                .containsEntry("server", "vpn.example.com")
                .containsEntry("user", "user")
                .containsEntry("password", "pw")
                .containsEntry("key", "secret")
                .containsEntry("type", "IKEv2")
                .doesNotContainKey("group");
    }

    @Test
    void vpn_type_ipsec_maps_to_pascal_spec_wire_form() {
        var vpn = new VpnConfig("vpn.example.com", "user", "grp", "pw", "secret", VpnType.IPSEC);
        var profile = new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRED_0,
                30, 3, "wss://x", null, vpn);

        useCase.setNetworkProfile(tenantId, station, 0, profile).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("connectionData");
        @SuppressWarnings("unchecked")
        Map<String, Object> vpnWire = (Map<String, Object>) conn.get("vpn");
        assertThat(vpnWire).containsEntry("type", "IPSec").containsEntry("group", "grp");
    }

    @Test
    void apn_only_required_fields_serialised() {
        var apn = new ApnConfig("internet", null, null, null, null, null, ApnAuthMethod.NONE);
        var profile = new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRELESS_0,
                60, 2, "wss://x", apn, null);

        useCase.setNetworkProfile(tenantId, station, 2, profile).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("connectionData");
        @SuppressWarnings("unchecked")
        Map<String, Object> apnWire = (Map<String, Object>) conn.get("apn");
        assertThat(apnWire)
                .containsOnlyKeys("apn", "apnAuthentication")
                .containsEntry("apn", "internet")
                .containsEntry("apnAuthentication", "NONE");
    }

    @Test
    void apn_full_fields_serialised_including_optional() {
        var apn = new ApnConfig("internet", "u", "p", 1234, "23430", true, ApnAuthMethod.CHAP);
        var profile = new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRELESS_0,
                60, 2, "wss://x", apn, null);

        useCase.setNetworkProfile(tenantId, station, 2, profile).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> conn = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("connectionData");
        @SuppressWarnings("unchecked")
        Map<String, Object> apnWire = (Map<String, Object>) conn.get("apn");
        assertThat(apnWire)
                .containsEntry("apn", "internet")
                .containsEntry("apnUserName", "u")
                .containsEntry("apnPassword", "p")
                .containsEntry("simPin", 1234)
                .containsEntry("preferredNetwork", "23430")
                .containsEntry("useOnlyPreferredNetwork", true)
                .containsEntry("apnAuthentication", "CHAP");
    }

    @Test
    void rejected_and_failed_status_are_propagated() {
        commandSender.setNextResponse(Map.of("status", "Rejected"));
        var profile = NetworkConnectionProfile.ofWebSocket(
                OcppVersion.OCPP_20, OcppInterface.WIRED_0, "wss://x", 30, 1);

        CommandResult r = useCase.setNetworkProfile(tenantId, station, 0, profile).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo("Rejected");

        commandSender.setNextResponse(Map.of("status", "Failed"));
        CommandResult r2 = useCase.setNetworkProfile(tenantId, station, 0, profile).join();
        assertThat(r2.status()).isEqualTo("Failed");
    }

    @Test
    void negative_configuration_slot_rejected() {
        var profile = NetworkConnectionProfile.ofWebSocket(
                OcppVersion.OCPP_20, OcppInterface.WIRED_0, "wss://x", 30, 1);

        assertThatThrownBy(() -> useCase.setNetworkProfile(tenantId, station, -1, profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configurationSlot");
    }

    @Test
    void invalid_security_profile_rejected_at_domain_construction() {
        assertThatThrownBy(() -> new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRED_0,
                30, 4, "wss://x", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("securityProfile");

        assertThatThrownBy(() -> new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRED_0,
                30, 0, "wss://x", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("securityProfile");
    }

    @Test
    void non_positive_message_timeout_rejected_at_domain_construction() {
        assertThatThrownBy(() -> new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRED_0,
                0, 1, "wss://x", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageTimeoutSeconds");
    }

    @Test
    void url_over_512_chars_rejected_at_domain_construction() {
        String longUrl = "wss://" + "a".repeat(520);
        assertThatThrownBy(() -> new NetworkConnectionProfile(
                OcppVersion.OCPP_20, OcppTransport.JSON, OcppInterface.WIRED_0,
                30, 1, longUrl, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ocppCsmsUrl");
    }
}
