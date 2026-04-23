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
import com.evlibre.server.core.domain.v201.ports.inbound.SetNetworkProfilePort;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SetNetworkProfileUseCaseV201 implements SetNetworkProfilePort {

    private static final Logger log = LoggerFactory.getLogger(SetNetworkProfileUseCaseV201.class);

    private final Ocpp201StationCommandSender commandSender;

    public SetNetworkProfileUseCaseV201(Ocpp201StationCommandSender commandSender) {
        this.commandSender = Objects.requireNonNull(commandSender);
    }

    @Override
    public CompletableFuture<CommandResult> setNetworkProfile(TenantId tenantId,
                                                                ChargePointIdentity stationIdentity,
                                                                int configurationSlot,
                                                                NetworkConnectionProfile profile) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(profile);
        if (configurationSlot < 0) {
            throw new IllegalArgumentException("configurationSlot must be >= 0, got " + configurationSlot);
        }

        Map<String, Object> payload = Map.of(
                "configurationSlot", configurationSlot,
                "connectionData", profileToWire(profile));

        log.info("Sending SetNetworkProfile(slot={}, version={}, iface={}) to {} (tenant: {})",
                configurationSlot, profile.ocppVersion(), profile.ocppInterface(),
                stationIdentity.value(), tenantId.value());

        return commandSender.sendCommand(tenantId, stationIdentity, "SetNetworkProfile", payload)
                .thenApply(response -> {
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    log.info("SetNetworkProfile response from {}: {}", stationIdentity.value(), status);
                    return new CommandResult(status, response);
                });
    }

    private static Map<String, Object> profileToWire(NetworkConnectionProfile profile) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ocppVersion", ocppVersionToWire(profile.ocppVersion()));
        out.put("ocppTransport", ocppTransportToWire(profile.ocppTransport()));
        out.put("ocppInterface", ocppInterfaceToWire(profile.ocppInterface()));
        out.put("messageTimeout", profile.messageTimeoutSeconds());
        out.put("securityProfile", profile.securityProfile());
        out.put("ocppCsmsUrl", profile.ocppCsmsUrl());
        if (profile.apn() != null) {
            out.put("apn", apnToWire(profile.apn()));
        }
        if (profile.vpn() != null) {
            out.put("vpn", vpnToWire(profile.vpn()));
        }
        return out;
    }

    private static Map<String, Object> apnToWire(ApnConfig apn) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("apn", apn.apn());
        if (apn.apnUserName() != null) {
            out.put("apnUserName", apn.apnUserName());
        }
        if (apn.apnPassword() != null) {
            out.put("apnPassword", apn.apnPassword());
        }
        if (apn.simPin() != null) {
            out.put("simPin", apn.simPin());
        }
        if (apn.preferredNetwork() != null) {
            out.put("preferredNetwork", apn.preferredNetwork());
        }
        if (apn.useOnlyPreferredNetwork() != null) {
            out.put("useOnlyPreferredNetwork", apn.useOnlyPreferredNetwork());
        }
        out.put("apnAuthentication", apnAuthToWire(apn.apnAuthentication()));
        return out;
    }

    private static Map<String, Object> vpnToWire(VpnConfig vpn) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("server", vpn.server());
        out.put("user", vpn.user());
        if (vpn.group() != null) {
            out.put("group", vpn.group());
        }
        out.put("password", vpn.password());
        out.put("key", vpn.key());
        out.put("type", vpnTypeToWire(vpn.type()));
        return out;
    }

    private static String ocppVersionToWire(OcppVersion v) {
        return switch (v) {
            case OCPP_12 -> "OCPP12";
            case OCPP_15 -> "OCPP15";
            case OCPP_16 -> "OCPP16";
            case OCPP_20 -> "OCPP20";
        };
    }

    private static String ocppTransportToWire(OcppTransport t) {
        return switch (t) {
            case JSON -> "JSON";
            case SOAP -> "SOAP";
        };
    }

    private static String ocppInterfaceToWire(OcppInterface i) {
        return switch (i) {
            case WIRED_0 -> "Wired0";
            case WIRED_1 -> "Wired1";
            case WIRED_2 -> "Wired2";
            case WIRED_3 -> "Wired3";
            case WIRELESS_0 -> "Wireless0";
            case WIRELESS_1 -> "Wireless1";
            case WIRELESS_2 -> "Wireless2";
            case WIRELESS_3 -> "Wireless3";
        };
    }

    private static String vpnTypeToWire(VpnType t) {
        return switch (t) {
            case IKE_V2 -> "IKEv2";
            case IPSEC -> "IPSec";
            case L2TP -> "L2TP";
            case PPTP -> "PPTP";
        };
    }

    private static String apnAuthToWire(ApnAuthMethod m) {
        return switch (m) {
            case CHAP -> "CHAP";
            case NONE -> "NONE";
            case PAP -> "PAP";
            case AUTO -> "AUTO";
        };
    }
}
