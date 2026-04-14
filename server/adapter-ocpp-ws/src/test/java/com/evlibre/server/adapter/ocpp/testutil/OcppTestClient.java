package com.evlibre.server.adapter.ocpp.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A WebSocket test client that connects to the OCPP server, boots a station,
 * stays connected, and can auto-respond to CSMS-initiated commands.
 * <p>
 * Usage:
 * <pre>
 *   var client = OcppTestClient.connect(vertx, harness, "CHARGER-001", "ocpp1.6").get();
 *   client.onCommand("Reset", payload -> Map.of("status", "Accepted"));
 *   // ... send a Reset command via harness.commandSender ...
 * </pre>
 */
public class OcppTestClient {

    public record ReceivedCommand(String messageId, String action, JsonNode payload) {}

    private final WebSocket ws;
    private final ObjectMapper objectMapper;
    private final Map<String, Function<JsonNode, Map<String, Object>>> commandHandlers = new ConcurrentHashMap<>();
    private final List<ReceivedCommand> receivedCommands = Collections.synchronizedList(new ArrayList<>());

    private OcppTestClient(WebSocket ws, ObjectMapper objectMapper) {
        this.ws = ws;
        this.objectMapper = objectMapper;
        ws.textMessageHandler(this::handleMessage);
    }

    /**
     * Connect to the test harness, send BootNotification, and return a connected client.
     */
    public static CompletableFuture<OcppTestClient> connect(Vertx vertx, OcppTestHarness harness,
                                                             String stationId, String subProtocol) {
        return connect(vertx, harness.port(), harness.objectMapper, "demo-tenant", stationId, subProtocol);
    }

    public static CompletableFuture<OcppTestClient> connect(Vertx vertx, int port, ObjectMapper objectMapper,
                                                             String tenantId, String stationId, String subProtocol) {
        CompletableFuture<OcppTestClient> result = new CompletableFuture<>();
        WebSocketClient wsClient = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setPort(port)
                .setHost("localhost")
                .setURI("/ocpp/" + tenantId + "/" + stationId)
                .addSubProtocol(subProtocol);

        wsClient.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                result.completeExceptionally(ar.cause());
                return;
            }
            WebSocket ws = ar.result();
            OcppTestClient client = new OcppTestClient(ws, objectMapper);

            // Send BootNotification and wait for response before returning
            String bootMsg;
            if ("ocpp1.6".equals(subProtocol)) {
                bootMsg = OcppMessages.bootNotification16("boot-init", "TestVendor", "TestModel");
            } else {
                bootMsg = OcppMessages.bootNotification201("boot-init", "TestVendor", "TestModel");
            }

            // Temporarily intercept the boot response
            client.bootFuture = result;
            ws.writeTextMessage(bootMsg);
        });

        return result.orTimeout(5, TimeUnit.SECONDS);
    }

    // Used during boot handshake only
    private volatile CompletableFuture<OcppTestClient> bootFuture;

    private void handleMessage(String msg) {
        try {
            JsonNode frame = objectMapper.readTree(msg);
            int messageTypeId = frame.get(0).asInt();

            if (messageTypeId == 3 && bootFuture != null) {
                // CALLRESULT for BootNotification
                CompletableFuture<OcppTestClient> bf = bootFuture;
                bootFuture = null;
                bf.complete(this);
                return;
            }

            if (messageTypeId == 2) {
                // CALL from CSMS — this is a command
                String messageId = frame.get(1).asText();
                String action = frame.get(2).asText();
                JsonNode payload = frame.get(3);

                receivedCommands.add(new ReceivedCommand(messageId, action, payload));

                Function<JsonNode, Map<String, Object>> handler = commandHandlers.get(action);
                if (handler != null) {
                    Map<String, Object> responsePayload = handler.apply(payload);
                    JsonNode responseNode = objectMapper.valueToTree(responsePayload);
                    String response = String.format("[3,\"%s\",%s]", messageId, responseNode.toString());
                    ws.writeTextMessage(response);
                } else {
                    // Default: respond with Accepted
                    String response = String.format("[3,\"%s\",{\"status\":\"Accepted\"}]", messageId);
                    ws.writeTextMessage(response);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle message: " + msg, e);
        }
    }

    /**
     * Register a handler for a CSMS-initiated command. The handler receives the payload
     * and returns the response payload map.
     */
    public OcppTestClient onCommand(String action, Function<JsonNode, Map<String, Object>> handler) {
        commandHandlers.put(action, handler);
        return this;
    }

    /**
     * Register a simple handler that always returns the given status.
     */
    public OcppTestClient onCommand(String action, String status) {
        return onCommand(action, payload -> Map.of("status", status));
    }

    public List<ReceivedCommand> receivedCommands() {
        return List.copyOf(receivedCommands);
    }

    public List<ReceivedCommand> receivedCommands(String action) {
        return receivedCommands.stream()
                .filter(c -> c.action().equals(action))
                .toList();
    }

    /**
     * Send a raw OCPP message (CS→CSMS) and return the response.
     */
    public CompletableFuture<JsonNode> send(String message) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        // Temporarily override the text handler to capture the response
        ws.textMessageHandler(msg -> {
            try {
                JsonNode parsed = objectMapper.readTree(msg);
                int typeId = parsed.get(0).asInt();
                if (typeId == 3 || typeId == 4) {
                    // Restore normal handler
                    ws.textMessageHandler(this::handleMessage);
                    future.complete(parsed);
                } else {
                    // This is a CALL from CSMS, handle normally and keep waiting
                    handleMessage(msg);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        ws.writeTextMessage(message);
        return future.orTimeout(5, TimeUnit.SECONDS);
    }

    public void close() {
        ws.close();
    }
}
