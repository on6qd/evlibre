package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.ServerWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the protocol enforcement in {@link OcppStationCommandSender}:
 * the v1.6 port must only dispatch to v1.6-negotiated sessions, and vice versa.
 */
class OcppStationCommandSenderTest {

    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity IDENT = new ChargePointIdentity("CS-1");

    private OcppSessionManager sessionManager;
    private OcppMessageCodec codec;
    private OcppPendingCallManager pendingCallManager;
    private ObjectMapper objectMapper;
    private RecordingWebSocket ws;
    private OcppStationCommandSender sender;

    @BeforeEach
    void setUp() {
        sessionManager = new OcppSessionManager();
        codec = new OcppMessageCodec(new ObjectMapper());
        pendingCallManager = new OcppPendingCallManager();
        objectMapper = new ObjectMapper();
        ws = new RecordingWebSocket();
        sender = new OcppStationCommandSender(sessionManager, codec, pendingCallManager, objectMapper);
    }

    @Test
    void v16_port_rejects_when_session_is_v201() {
        sessionManager.register(new OcppSession(TENANT, IDENT, OcppProtocol.OCPP_201, ws.asServerWebSocket()));

        assertThatThrownBy(() -> sender.v16()
                .sendCommand(TENANT, IDENT, "ChangeAvailability", Map.of("connectorId", 1, "type", "Inoperative"))
                .get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("OCPP_16")
                .hasMessageContaining("OCPP_201");

        assertThat(ws.sentMessages).isEmpty();
    }

    @Test
    void v201_port_rejects_when_session_is_v16() {
        sessionManager.register(new OcppSession(TENANT, IDENT, OcppProtocol.OCPP_16, ws.asServerWebSocket()));

        assertThatThrownBy(() -> sender.v201()
                .sendCommand(TENANT, IDENT, "GetBaseReport", Map.of("requestId", 0, "reportBase", "FullInventory"))
                .get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("OCPP_201")
                .hasMessageContaining("OCPP_16");

        assertThat(ws.sentMessages).isEmpty();
    }

    @Test
    void v16_port_sends_when_session_is_v16() {
        sessionManager.register(new OcppSession(TENANT, IDENT, OcppProtocol.OCPP_16, ws.asServerWebSocket()));

        sender.v16().sendCommand(TENANT, IDENT, "ChangeAvailability",
                Map.of("connectorId", 1, "type", "Inoperative"));

        assertThat(ws.sentMessages).hasSize(1);
        assertThat(ws.sentMessages.get(0)).contains("\"ChangeAvailability\"");
    }

    @Test
    void v201_port_sends_when_session_is_v201() {
        sessionManager.register(new OcppSession(TENANT, IDENT, OcppProtocol.OCPP_201, ws.asServerWebSocket()));

        sender.v201().sendCommand(TENANT, IDENT, "GetBaseReport",
                Map.of("requestId", 0, "reportBase", "FullInventory"));

        assertThat(ws.sentMessages).hasSize(1);
        assertThat(ws.sentMessages.get(0)).contains("\"GetBaseReport\"");
    }

    @Test
    void both_ports_reject_when_station_not_connected() {
        // No session registered for IDENT.
        assertThatThrownBy(() -> sender.v16().sendCommand(TENANT, IDENT, "Reset", Map.of("type", "Soft"))
                .get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Station not connected");
        assertThatThrownBy(() -> sender.v201().sendCommand(TENANT, IDENT, "Reset", Map.of("type", "Immediate"))
                .get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Station not connected");
    }

    /**
     * Minimal recorder for {@link ServerWebSocket#writeTextMessage(String)} — the only
     * method this class exercises. Constructed via JDK dynamic proxy so the test does
     * not need a heavyweight Vert.x fixture.
     */
    private static final class RecordingWebSocket {
        final java.util.List<String> sentMessages = new java.util.ArrayList<>();

        ServerWebSocket asServerWebSocket() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("writeTextMessage".equals(method.getName()) && args != null && args.length == 1
                        && args[0] instanceof String s) {
                    sentMessages.add(s);
                    return null;
                }
                // Default for returning Future/primitive — null is fine because the sender
                // doesn't chain on this method in the happy path.
                return null;
            };
            return (ServerWebSocket) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { ServerWebSocket.class },
                    handler);
        }
    }
}
