package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.MessageTypeId;
import com.evlibre.common.ocpp.OcppErrorCode;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.HandleHeartbeatPort;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceEventPublisher;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OcppWebSocketVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(OcppWebSocketVerticle.class);

    private static final MessageTraceStorePort NOOP_TRACE_STORE = new MessageTraceStorePort() {
        @Override public void record(TenantId t, ChargePointIdentity s, MessageTraceEntry e) {}
        @Override public List<MessageTraceEntry> recent(TenantId t, ChargePointIdentity s) { return List.of(); }
    };
    private static final MessageTraceEventPublisher NOOP_TRACE_EVENTS = (t, s, e) -> {};

    private final int port;
    private final int pingInterval;
    private final OcppMessageCodec codec;
    private final OcppSchemaValidator schemaValidator;
    private final OcppMessageDispatcher dispatcher;
    private final OcppSessionManager sessionManager;
    private final OcppProtocolNegotiator protocolNegotiator;
    private final OcppPendingCallManager pendingCallManager;
    private final StationEventPublisher stationEventPublisher;
    private final HandleHeartbeatPort heartbeatPort;
    private final MessageTraceStorePort traceStore;
    private final MessageTraceEventPublisher traceEvents;
    private HttpServer httpServer;

    public OcppWebSocketVerticle(int port,
                                  int pingInterval,
                                  OcppMessageCodec codec,
                                  OcppSchemaValidator schemaValidator,
                                  OcppMessageDispatcher dispatcher,
                                  OcppSessionManager sessionManager,
                                  OcppProtocolNegotiator protocolNegotiator,
                                  OcppPendingCallManager pendingCallManager,
                                  StationEventPublisher stationEventPublisher) {
        this(port, pingInterval, codec, schemaValidator, dispatcher, sessionManager,
                protocolNegotiator, pendingCallManager, stationEventPublisher, null,
                NOOP_TRACE_STORE, NOOP_TRACE_EVENTS);
    }

    public OcppWebSocketVerticle(int port,
                                  int pingInterval,
                                  OcppMessageCodec codec,
                                  OcppSchemaValidator schemaValidator,
                                  OcppMessageDispatcher dispatcher,
                                  OcppSessionManager sessionManager,
                                  OcppProtocolNegotiator protocolNegotiator,
                                  OcppPendingCallManager pendingCallManager,
                                  StationEventPublisher stationEventPublisher,
                                  HandleHeartbeatPort heartbeatPort) {
        this(port, pingInterval, codec, schemaValidator, dispatcher, sessionManager,
                protocolNegotiator, pendingCallManager, stationEventPublisher, heartbeatPort,
                NOOP_TRACE_STORE, NOOP_TRACE_EVENTS);
    }

    public OcppWebSocketVerticle(int port,
                                  int pingInterval,
                                  OcppMessageCodec codec,
                                  OcppSchemaValidator schemaValidator,
                                  OcppMessageDispatcher dispatcher,
                                  OcppSessionManager sessionManager,
                                  OcppProtocolNegotiator protocolNegotiator,
                                  OcppPendingCallManager pendingCallManager,
                                  StationEventPublisher stationEventPublisher,
                                  HandleHeartbeatPort heartbeatPort,
                                  MessageTraceStorePort traceStore,
                                  MessageTraceEventPublisher traceEvents) {
        this.port = port;
        this.pingInterval = pingInterval;
        this.codec = codec;
        this.schemaValidator = schemaValidator;
        this.dispatcher = dispatcher;
        this.sessionManager = sessionManager;
        this.protocolNegotiator = protocolNegotiator;
        this.pendingCallManager = pendingCallManager;
        this.stationEventPublisher = stationEventPublisher;
        this.heartbeatPort = heartbeatPort;
        this.traceStore = Objects.requireNonNull(traceStore);
        this.traceEvents = Objects.requireNonNull(traceEvents);
    }

    @Override
    public void start(Promise<Void> startPromise) {
        httpServer = vertx.createHttpServer(new HttpServerOptions()
                .setWebSocketSubProtocols(List.of(
                        OcppProtocol.OCPP_16.subProtocol(),
                        OcppProtocol.OCPP_201.subProtocol()
                )));

        Router router = Router.router(vertx);

        router.route("/ocpp/:tenantId/:stationId").handler(ctx -> {
            String offeredSubProtocols = ctx.request().getHeader("Sec-WebSocket-Protocol");
            log.debug("HTTP request to {}: upgrade={}, sub-protocols=[{}]", ctx.request().path(),
                    ctx.request().getHeader("Upgrade"), offeredSubProtocols);
            ctx.request().toWebSocket().onSuccess(ws -> {
                handleWebSocketConnection(ws, ctx.pathParam("tenantId"), ctx.pathParam("stationId"),
                        offeredSubProtocols);
            }).onFailure(err -> {
                log.error("WebSocket upgrade failed for {}: {}", ctx.request().path(), err.getMessage());
            });
        });

        httpServer.requestHandler(router).listen(port)
                .onSuccess(server -> {
                    log.info("OCPP WebSocket server listening on port {} (ping interval: {}s)",
                            server.actualPort(), pingInterval);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (httpServer != null) {
            httpServer.close().onComplete(ar -> stopPromise.complete());
        } else {
            stopPromise.complete();
        }
    }

    public int actualPort() {
        return httpServer != null ? httpServer.actualPort() : -1;
    }

    private void handleWebSocketConnection(ServerWebSocket ws, String tenantIdStr, String stationIdStr,
                                            String offeredSubProtocols) {
        // Negotiate protocol
        String selectedSubProtocol = ws.subProtocol();
        OcppProtocol protocol = OcppProtocol.fromSubProtocol(selectedSubProtocol);

        if (protocol == null) {
            log.warn("No matching OCPP sub-protocol for connection from {} (offered: [{}], selected: [{}])",
                    stationIdStr, offeredSubProtocols, selectedSubProtocol);
            recordLifecycleSafely(tenantIdStr, stationIdStr,
                    LifecycleKind.SUBPROTOCOL_REJECTED,
                    "offered=[" + offeredSubProtocols + "]");
            ws.close((short) 1002, "No matching OCPP sub-protocol");
            return;
        }

        TenantId tenantId;
        ChargePointIdentity stationIdentity;
        try {
            tenantId = new TenantId(tenantIdStr);
            stationIdentity = new ChargePointIdentity(stationIdStr);
        } catch (Exception e) {
            log.warn("Invalid connection parameters: {}", e.getMessage());
            ws.close((short) 1002, "Invalid parameters");
            return;
        }

        // Reject duplicate connections (like CitrineOS ConnectedStationFilter)
        if (sessionManager.isConnected(tenantId, stationIdentity)) {
            log.warn("Duplicate connection from {} (tenant: {}), rejecting",
                    stationIdStr, tenantIdStr);
            ws.close((short) 1008, "Already connected");
            return;
        }

        log.info("Station connected: {} (tenant: {}, protocol: {})", stationIdStr, tenantIdStr, protocol);

        OcppSession session = new OcppSession(tenantId, stationIdentity, protocol, ws);
        sessionManager.register(session);
        stationEventPublisher.stationUpdated(tenantId, stationIdentity);
        recordTrace(tenantId, stationIdentity, new MessageTraceEntry.Lifecycle(
                Instant.now(), LifecycleKind.CONNECTED, protocol.subProtocol()));

        ws.textMessageHandler(message -> handleMessage(session, message));

        // Pong handler: cancel deadline, schedule next ping
        ws.pongHandler(buf -> {
            log.trace("Pong received from {}", stationIdStr);
            cancelDeadlineTimer(tenantId, stationIdentity);
            schedulePing(tenantId, stationIdentity, ws);
        });

        ws.closeHandler(v -> {
            log.info("Station disconnected: {} (tenant: {})", stationIdStr, tenantIdStr);
            cancelAllTimers(tenantId, stationIdentity);
            sessionManager.unregister(tenantId, stationIdentity);
            stationEventPublisher.stationUpdated(tenantId, stationIdentity);
            recordTrace(tenantId, stationIdentity, new MessageTraceEntry.Lifecycle(
                    Instant.now(), LifecycleKind.DISCONNECTED, formatCloseInfo(ws)));
        });

        ws.exceptionHandler(err ->
                log.error("WebSocket error for station {}: {}", stationIdStr, err.getMessage()));

        // Start ping/pong cycle
        schedulePing(tenantId, stationIdentity, ws);
    }

    private void schedulePing(TenantId tenantId, ChargePointIdentity stationIdentity, ServerWebSocket ws) {
        long timerId = vertx.setTimer(pingInterval * 1000L, id -> {
            if (!sessionManager.isConnected(tenantId, stationIdentity)) {
                return;
            }
            // Send ping
            ws.writePing(Buffer.buffer());
            log.trace("Ping sent to {}", stationIdentity.value());

            // Set deadline: if no pong within pingInterval, close
            long deadlineId = vertx.setTimer(pingInterval * 1000L, deadlineTimerId -> {
                if (sessionManager.isConnected(tenantId, stationIdentity)) {
                    log.warn("No pong from {} within {}s, closing connection",
                            stationIdentity.value(), pingInterval);
                    ws.close((short) 1011, "Pong timeout");
                }
            });
            sessionManager.setDeadlineTimerId(tenantId, stationIdentity, deadlineId);
        });
        sessionManager.setPingTimerId(tenantId, stationIdentity, timerId);
    }

    private void cancelDeadlineTimer(TenantId tenantId, ChargePointIdentity stationIdentity) {
        var state = sessionManager.getPingState(tenantId, stationIdentity);
        if (state.deadlineTimerId() != -1) {
            vertx.cancelTimer(state.deadlineTimerId());
        }
    }

    private void cancelAllTimers(TenantId tenantId, ChargePointIdentity stationIdentity) {
        var state = sessionManager.getPingState(tenantId, stationIdentity);
        if (state.pingTimerId() != -1) {
            vertx.cancelTimer(state.pingTimerId());
        }
        if (state.deadlineTimerId() != -1) {
            vertx.cancelTimer(state.deadlineTimerId());
        }
    }

    private void handleMessage(OcppSession session, String rawMessage) {
        log.debug("OCPP IN  [{}] {}", session.stationIdentity().value(), rawMessage);
        OcppMessageCodec.ParsedMessage parsed;
        try {
            parsed = codec.parse(rawMessage);
        } catch (OcppMessageCodec.OcppMessageParseException e) {
            log.warn("Failed to parse OCPP message from {}: {}", session.stationIdentity().value(), e.getMessage());
            return;
        }

        recordInboundFrame(session, parsed, rawMessage);

        // OCPP 1.6 §4.5.1: the CSMS SHOULD treat any PDU as a liveness signal — just as
        // it would a Heartbeat. Update lastSeen eagerly so the dashboard reflects actual
        // recent activity, not only explicit Heartbeats.
        if (heartbeatPort != null && sessionManager.isAccepted(session.tenantId(), session.stationIdentity())) {
            heartbeatPort.heartbeat(session.tenantId(), session.stationIdentity());
        }

        if (parsed.typeId() == MessageTypeId.CALL) {
            handleCall(session, (OcppCallMessage) parsed.message());
        } else if (parsed.typeId() == MessageTypeId.CALL_RESULT) {
            handleCallResult((OcppCallResultMessage) parsed.message());
        } else if (parsed.typeId() == MessageTypeId.CALL_ERROR) {
            handleCallError((OcppCallErrorMessage) parsed.message());
        }
    }

    private void handleCall(OcppSession session, OcppCallMessage call) {
        // OCPP 1.6 §4.2: reject any action other than BootNotification from a station
        // that hasn't been accepted yet. Recommended error: SecurityError.
        if (!"BootNotification".equals(call.action())
                && !sessionManager.isAccepted(session.tenantId(), session.stationIdentity())) {
            log.warn("Pre-boot message {} from {} rejected with SecurityError",
                    call.action(), session.stationIdentity().value());
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.SECURITY_ERROR,
                    "Station must send BootNotification before any other action");
            writeOutbound(session, FrameType.CALL_ERROR, call.messageId(), error);
            return;
        }

        // Validate schema
        var validationResult = schemaValidator.validate(session.protocol(), call.action(), call.payload());
        if (!validationResult.isValid()) {
            log.warn("Schema validation failed for {} from {}: {}",
                    call.action(), session.stationIdentity().value(), validationResult.errorMessage());
            String error = codec.buildCallError(call.messageId(),
                    validationResult.errorCode(), validationResult.errorMessage());
            writeOutbound(session, FrameType.CALL_ERROR, call.messageId(), error);
            return;
        }

        // Dispatch to handler
        Optional<OcppMessageHandler> handler = dispatcher.getHandler(session.protocol(), call.action());
        if (handler.isEmpty()) {
            log.warn("No handler for action {} (protocol {})", call.action(), session.protocol());
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.NOT_IMPLEMENTED, "Action not supported: " + call.action());
            writeOutbound(session, FrameType.CALL_ERROR, call.messageId(), error);
            return;
        }

        try {
            OcppMessageHandler h = handler.get();
            JsonNode responsePayload = h.handle(session, call.messageId(), call.payload());

            // Self-check: the payload we're about to send to the station must itself
            // conform to the response schema. A handler producing a malformed response
            // is a bug we want to surface immediately — return a CALL_ERROR instead of
            // shipping bad data to the station.
            var responseCheck = schemaValidator.validateResponse(session.protocol(), call.action(), responsePayload);
            if (!responseCheck.isValid()) {
                log.error("Our {} response to {} failed schema validation: {}",
                        call.action(), session.stationIdentity().value(), responseCheck.errorMessage());
                String error = codec.buildCallError(call.messageId(),
                        OcppErrorCode.INTERNAL_ERROR,
                        "Server-generated response failed schema validation");
                writeOutbound(session, FrameType.CALL_ERROR, call.messageId(), error);
                return;
            }

            String response = codec.buildCallResult(call.messageId(), responsePayload);
            writeOutbound(session, FrameType.CALL_RESULT, call.messageId(), response);
            h.afterResponse(session);
        } catch (Exception e) {
            log.error("Error handling {} from {}: {}", call.action(), session.stationIdentity().value(), e.getMessage(), e);
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.INTERNAL_ERROR, "Internal error processing " + call.action());
            writeOutbound(session, FrameType.CALL_ERROR, call.messageId(), error);
        }
    }

    private void handleCallResult(OcppCallResultMessage result) {
        log.debug("Received CALLRESULT for message {}", result.messageId());
        pendingCallManager.resolveCallResult(result.messageId(), result.payload());
    }

    private void handleCallError(OcppCallErrorMessage error) {
        log.warn("Received CALLERROR for message {}: {} - {}",
                error.messageId(), error.errorCode(), error.errorDescription());
        pendingCallManager.resolveCallError(error.messageId(), error.errorCode().value(), error.errorDescription());
    }

    private void writeOutbound(OcppSession session, FrameType type, String messageId, String message) {
        log.debug("OCPP OUT [{}] {}", session.stationIdentity().value(), message);
        session.webSocket().writeTextMessage(message);
        recordTrace(session.tenantId(), session.stationIdentity(), new MessageTraceEntry.OcppFrame(
                Instant.now(), Direction.OUT, type, null, messageId, message));
    }

    private void recordInboundFrame(OcppSession session, OcppMessageCodec.ParsedMessage parsed, String rawMessage) {
        FrameType type;
        String action = null;
        String messageId;
        if (parsed.typeId() == MessageTypeId.CALL) {
            type = FrameType.CALL;
            OcppCallMessage call = (OcppCallMessage) parsed.message();
            action = call.action();
            messageId = call.messageId();
        } else if (parsed.typeId() == MessageTypeId.CALL_RESULT) {
            type = FrameType.CALL_RESULT;
            messageId = ((OcppCallResultMessage) parsed.message()).messageId();
        } else if (parsed.typeId() == MessageTypeId.CALL_ERROR) {
            type = FrameType.CALL_ERROR;
            messageId = ((OcppCallErrorMessage) parsed.message()).messageId();
        } else {
            return;
        }
        recordTrace(session.tenantId(), session.stationIdentity(), new MessageTraceEntry.OcppFrame(
                Instant.now(), Direction.IN, type, action, messageId, rawMessage));
    }

    private void recordTrace(TenantId tenant, ChargePointIdentity station, MessageTraceEntry entry) {
        traceStore.record(tenant, station, entry);
        traceEvents.messageRecorded(tenant, station, entry);
    }

    private void recordLifecycleSafely(String tenantIdStr, String stationIdStr,
                                        LifecycleKind kind, String detail) {
        try {
            recordTrace(new TenantId(tenantIdStr), new ChargePointIdentity(stationIdStr),
                    new MessageTraceEntry.Lifecycle(Instant.now(), kind, detail));
        } catch (Exception ignored) {
            // Pre-handshake rejection with malformed identifiers — log already covers it.
        }
    }

    private static String formatCloseInfo(ServerWebSocket ws) {
        Short code = ws.closeStatusCode();
        String reason = ws.closeReason();
        if (code == null && (reason == null || reason.isBlank())) {
            return "no close frame";
        }
        StringBuilder sb = new StringBuilder();
        if (code != null) sb.append(code);
        if (reason != null && !reason.isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(reason);
        }
        return sb.toString();
    }
}
