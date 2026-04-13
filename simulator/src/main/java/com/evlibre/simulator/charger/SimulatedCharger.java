package com.evlibre.simulator.charger;

import com.evlibre.simulator.config.ChargerDefinition;
import com.evlibre.simulator.config.DefaultsConfig;
import com.evlibre.simulator.protocol.MessageBuilder;
import com.evlibre.simulator.protocol.OcppClientCodec;
import com.evlibre.simulator.protocol.OcppClientCodec.ParsedMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SimulatedCharger {

    private static final Logger log = LoggerFactory.getLogger(SimulatedCharger.class);

    private final Vertx vertx;
    private final ChargerDefinition def;
    private final DefaultsConfig defaults;
    private final OcppClientCodec codec;
    private final MessageBuilder msg;
    private final ObjectMapper om;

    private final List<Evse> evses;
    private ChargerState state = ChargerState.DISCONNECTED;
    private WebSocket ws;
    private final Map<String, CompletableFuture<ParsedMessage>> pendingCalls = new ConcurrentHashMap<>();
    private Long heartbeatTimer;
    private Long behaviorTimer;
    private Long meterTimer;
    private int seqNo = 0;
    private String v201TxId; // for 2.0.1 transaction tracking

    public SimulatedCharger(Vertx vertx, ChargerDefinition def, DefaultsConfig defaults, ObjectMapper om) {
        this.vertx = vertx;
        this.def = def;
        this.defaults = defaults;
        this.om = om;
        this.codec = new OcppClientCodec(om);
        this.msg = new MessageBuilder(om, def.protocol());

        this.evses = new ArrayList<>();
        for (int i = 1; i <= def.evseCount(); i++) {
            evses.add(new Evse(i, i));
        }
    }

    public String id() { return def.id(); }
    public ChargerState state() { return state; }

    // --- Lifecycle ---

    public void connect() {
        state = ChargerState.DISCONNECTED;
        URI uri = URI.create(defaults.serverUrl());
        String path = "/ocpp/" + def.tenant() + "/" + def.id();

        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setURI(path)
                .addSubProtocol(def.protocol());

        client.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                log.error("[{}] Connection failed: {}", def.id(), ar.cause().getMessage());
                scheduleReconnect();
                return;
            }

            ws = ar.result();
            log.info("[{}] Connected ({})", def.id(), def.protocol());

            ws.textMessageHandler(this::onMessage);
            ws.closeHandler(v -> {
                log.info("[{}] Disconnected", def.id());
                state = ChargerState.DISCONNECTED;
                cancelTimers();
            });

            boot();
        });
    }

    public void disconnect() {
        cancelTimers();
        if (ws != null) {
            ws.close();
            ws = null;
        }
        state = ChargerState.DISCONNECTED;
    }

    // --- Boot sequence ---

    private void boot() {
        state = ChargerState.BOOTING;
        log.info("[{}] {} -> BOOTING", def.id(), def.tenant());

        sendCall("BootNotification", msg.bootNotification(def.vendor(), def.model()))
                .thenAccept(resp -> {
                    String status = resp.payload().path("status").asText();
                    if ("Accepted".equals(status)) {
                        state = ChargerState.AVAILABLE;
                        log.info("[{}] BOOTING -> AVAILABLE", def.id());

                        // Send initial status for each EVSE
                        for (Evse evse : evses) {
                            send("StatusNotification",
                                    msg.statusNotification(evse.id(), evse.connectorId(), "Available", Instant.now()));
                        }

                        startHeartbeat();
                        startBehavior();
                    } else {
                        log.warn("[{}] Boot rejected: {}", def.id(), status);
                        scheduleReconnect();
                    }
                });
    }

    // --- Heartbeat ---

    private void startHeartbeat() {
        heartbeatTimer = vertx.setPeriodic(defaults.heartbeatInterval() * 1000L, id -> {
            sendCall("Heartbeat", msg.heartbeat());
        });
    }

    // --- Behavior ---

    private void startBehavior() {
        switch (def.behavior()) {
            case AUTO_CHARGE -> scheduleAutoCharge();
            case ERROR_PRONE -> scheduleErrorProne();
            case IDLE -> {} // just heartbeat + commands
        }
    }

    private void scheduleAutoCharge() {
        long delayMs = randomBetween(30_000, 120_000);
        behaviorTimer = vertx.setTimer(delayMs, id -> runAutoCharge());
    }

    private void runAutoCharge() {
        Evse available = evses.stream().filter(Evse::isAvailable).findFirst().orElse(null);
        if (available == null) {
            scheduleAutoCharge(); // try again later
            return;
        }

        String idTag = randomIdTag();
        Instant now = Instant.now();

        // Authorize -> Start -> MeterValues -> Stop
        sendCall("Authorize", msg.authorize(idTag)).thenCompose(authResp -> {
            log.info("[{}] Auto-charge: authorized {} on EVSE {}", def.id(), idTag, available.id());

            if (msg.is201()) {
                v201TxId = "tx-" + System.nanoTime();
                seqNo = 0;
                return sendCall("TransactionEvent",
                        msg.transactionEvent("Started", v201TxId, "Authorized",
                                seqNo++, available.connectorId(), idTag, available.meterWh(), now));
            } else {
                return sendCall("StartTransaction",
                        msg.startTransaction(available.connectorId(), idTag, available.meterWh(), now));
            }
        }).thenAccept(startResp -> {
            int txId = 0;
            if (!msg.is201()) {
                txId = startResp.payload().path("transactionId").asInt();
            }
            available.startCharging(txId, idTag);
            state = ChargerState.CHARGING;
            log.info("[{}] AVAILABLE -> CHARGING (EVSE {}, tag={})", def.id(), available.id(), idTag);

            send("StatusNotification",
                    msg.statusNotification(available.id(), available.connectorId(), "Charging", Instant.now()));

            startMeterValues(available);

            // Schedule stop after random duration
            long chargeDurationMs = randomBetween(60_000, 300_000);
            vertx.setTimer(chargeDurationMs, stopId -> stopCharging(available));
        });
    }

    private void startMeterValues(Evse evse) {
        meterTimer = vertx.setPeriodic(defaults.meterInterval() * 1000L, id -> {
            if (!evse.isCharging()) {
                vertx.cancelTimer(id);
                return;
            }

            // ~7-11 kW equivalent per interval
            long deltaWh = randomBetween(
                    (long)(7000.0 / 3600 * defaults.meterInterval()),
                    (long)(11000.0 / 3600 * defaults.meterInterval()));
            evse.incrementMeter(deltaWh);

            Instant now = Instant.now();
            if (msg.is201()) {
                sendCall("TransactionEvent",
                        msg.transactionEvent("Updated", v201TxId, "MeterValuePeriodic",
                                seqNo++, evse.connectorId(), null, evse.meterWh(), now));
            } else {
                send("MeterValues",
                        msg.meterValues(evse.connectorId(), evse.activeTransactionId(), evse.meterWh(), now));
            }

            log.debug("[{}] MeterValues EVSE {}: {} Wh", def.id(), evse.id(), evse.meterWh());
        });
    }

    private void stopCharging(Evse evse) {
        if (!evse.isCharging()) return;

        Instant now = Instant.now();
        log.info("[{}] CHARGING -> AVAILABLE (EVSE {}, {} Wh)", def.id(), evse.id(), evse.meterWh());

        if (msg.is201()) {
            sendCall("TransactionEvent",
                    msg.stopTransaction(0, v201TxId, evse.activeIdTag(), evse.meterWh(), now, "EVDisconnected"));
        } else {
            sendCall("StopTransaction",
                    msg.stopTransaction(evse.activeTransactionId(), null, evse.activeIdTag(),
                            evse.meterWh(), now, "EVDisconnected"));
        }

        evse.stopCharging();
        evse.setAvailable();
        state = evses.stream().anyMatch(Evse::isCharging) ? ChargerState.CHARGING : ChargerState.AVAILABLE;

        send("StatusNotification",
                msg.statusNotification(evse.id(), evse.connectorId(), "Available", now));

        // Schedule next charge cycle
        scheduleAutoCharge();
    }

    private void scheduleErrorProne() {
        long delayMs = randomBetween(60_000, 180_000);
        behaviorTimer = vertx.setTimer(delayMs, id -> {
            Evse evse = evses.get(ThreadLocalRandom.current().nextInt(evses.size()));
            if (evse.isAvailable()) {
                evse.setFaulted();
                log.info("[{}] FAULT injected on EVSE {}", def.id(), evse.id());
                send("StatusNotification",
                        msg.statusNotification(evse.id(), evse.connectorId(), "Faulted", Instant.now()));

                // Recover after 30-60s
                vertx.setTimer(randomBetween(30_000, 60_000), recId -> {
                    evse.setAvailable();
                    log.info("[{}] EVSE {} recovered", def.id(), evse.id());
                    send("StatusNotification",
                            msg.statusNotification(evse.id(), evse.connectorId(), "Available", Instant.now()));
                });
            }
            scheduleErrorProne();
        });
    }

    // --- Command handling (CSMS -> CS) ---

    private void handleCommand(ParsedMessage command) {
        String action = command.action();
        JsonNode payload = command.payload();
        log.info("[{}] Received command: {}", def.id(), action);

        JsonNode response = switch (action) {
            case "RemoteStartTransaction" -> handleRemoteStart(payload);
            case "RemoteStopTransaction" -> handleRemoteStop(payload);
            case "GetConfiguration" -> msg.getConfiguration();
            case "ChangeConfiguration" -> msg.changeConfigurationAccepted();
            case "Reset" -> handleReset();
            default -> {
                log.warn("[{}] Unknown command: {}", def.id(), action);
                yield om.createObjectNode().put("status", "NotImplemented");
            }
        };

        String frame = codec.buildCallResult(command.messageId(), response);
        ws.writeTextMessage(frame);
    }

    private JsonNode handleRemoteStart(JsonNode payload) {
        int connectorId = payload.path("connectorId").asInt(1);
        String idTag = payload.path("idTag").asText("REMOTE");

        Evse target = evses.stream()
                .filter(e -> e.connectorId() == connectorId && e.isAvailable())
                .findFirst()
                .orElse(evses.stream().filter(Evse::isAvailable).findFirst().orElse(null));

        if (target == null) {
            return om.createObjectNode().put("status", "Rejected");
        }

        log.info("[{}] RemoteStart on EVSE {} with tag {}", def.id(), target.id(), idTag);

        // Start charging asynchronously
        vertx.runOnContext(v -> {
            Instant now = Instant.now();
            if (msg.is201()) {
                v201TxId = "tx-" + System.nanoTime();
                seqNo = 0;
                sendCall("TransactionEvent",
                        msg.transactionEvent("Started", v201TxId, "RemoteStart",
                                seqNo++, target.connectorId(), idTag, target.meterWh(), now));
            } else {
                sendCall("StartTransaction",
                        msg.startTransaction(target.connectorId(), idTag, target.meterWh(), now))
                        .thenAccept(resp -> {
                            int txId = resp.payload().path("transactionId").asInt();
                            target.startCharging(txId, idTag);
                        });
            }

            if (msg.is201()) {
                target.startCharging(0, idTag);
            }
            state = ChargerState.CHARGING;

            send("StatusNotification",
                    msg.statusNotification(target.id(), target.connectorId(), "Charging", now));
            startMeterValues(target);
        });

        return msg.remoteStartAccepted();
    }

    private JsonNode handleRemoteStop(JsonNode payload) {
        int transactionId = payload.path("transactionId").asInt();

        Evse target = evses.stream()
                .filter(e -> e.isCharging() && (e.activeTransactionId() != null && e.activeTransactionId() == transactionId))
                .findFirst()
                .orElse(evses.stream().filter(Evse::isCharging).findFirst().orElse(null));

        if (target == null) {
            return om.createObjectNode().put("status", "Rejected");
        }

        log.info("[{}] RemoteStop on EVSE {}", def.id(), target.id());
        vertx.runOnContext(v -> stopCharging(target));
        return msg.remoteStopAccepted();
    }

    private JsonNode handleReset() {
        log.info("[{}] Reset requested, reconnecting in 3s", def.id());
        vertx.setTimer(3000, id -> {
            disconnect();
            connect();
        });
        return msg.resetAccepted();
    }

    // --- Messaging ---

    private CompletableFuture<ParsedMessage> sendCall(String action, JsonNode payload) {
        String messageId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String frame = codec.buildCall(messageId, action, payload);

        CompletableFuture<ParsedMessage> future = new CompletableFuture<>();
        pendingCalls.put(messageId, future);

        if (ws != null) {
            ws.writeTextMessage(frame);
        } else {
            future.completeExceptionally(new IllegalStateException("Not connected"));
        }

        // Timeout after 30s
        vertx.setTimer(30_000, id -> {
            if (pendingCalls.remove(messageId) != null) {
                future.completeExceptionally(new RuntimeException("Timeout waiting for " + action));
            }
        });

        return future;
    }

    private void send(String action, JsonNode payload) {
        if (ws != null) {
            String frame = codec.buildCall(action, payload);
            ws.writeTextMessage(frame);
        }
    }

    private void onMessage(String raw) {
        try {
            ParsedMessage parsed = codec.parse(raw);

            switch (parsed.type()) {
                case CALL_RESULT -> {
                    CompletableFuture<ParsedMessage> pending = pendingCalls.remove(parsed.messageId());
                    if (pending != null) {
                        pending.complete(parsed);
                    }
                }
                case CALL_ERROR -> {
                    CompletableFuture<ParsedMessage> pending = pendingCalls.remove(parsed.messageId());
                    if (pending != null) {
                        log.warn("[{}] Error response for {}: {}", def.id(), parsed.messageId(), parsed.action());
                        pending.completeExceptionally(
                                new RuntimeException("CALLERROR: " + parsed.action()));
                    }
                }
                case CALL -> handleCommand(parsed);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to parse message: {}", def.id(), e.getMessage());
        }
    }

    // --- Utilities ---

    private void cancelTimers() {
        if (heartbeatTimer != null) { vertx.cancelTimer(heartbeatTimer); heartbeatTimer = null; }
        if (behaviorTimer != null) { vertx.cancelTimer(behaviorTimer); behaviorTimer = null; }
        if (meterTimer != null) { vertx.cancelTimer(meterTimer); meterTimer = null; }
    }

    private void scheduleReconnect() {
        log.info("[{}] Reconnecting in 10s", def.id());
        vertx.setTimer(10_000, id -> connect());
    }

    private String randomIdTag() {
        List<String> tags = def.idTags();
        return tags.get(ThreadLocalRandom.current().nextInt(tags.size()));
    }

    private static long randomBetween(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
