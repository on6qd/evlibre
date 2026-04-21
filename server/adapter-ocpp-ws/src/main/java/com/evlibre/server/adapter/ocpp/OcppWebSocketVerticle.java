package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.MessageTypeId;
import com.evlibre.common.ocpp.OcppErrorCode;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.ports.inbound.HandleHeartbeatPort;
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

import java.util.List;
import java.util.Optional;

public class OcppWebSocketVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(OcppWebSocketVerticle.class);

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
                protocolNegotiator, pendingCallManager, stationEventPublisher, null);
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
            log.debug("HTTP request to {}: upgrade={}", ctx.request().path(),
                    ctx.request().getHeader("Upgrade"));
            ctx.request().toWebSocket().onSuccess(ws -> {
                handleWebSocketConnection(ws, ctx.pathParam("tenantId"), ctx.pathParam("stationId"));
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

    private void handleWebSocketConnection(ServerWebSocket ws, String tenantIdStr, String stationIdStr) {
        // Negotiate protocol
        String selectedSubProtocol = ws.subProtocol();
        OcppProtocol protocol = OcppProtocol.fromSubProtocol(selectedSubProtocol);

        if (protocol == null) {
            log.warn("No matching OCPP sub-protocol for connection from {}", stationIdStr);
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
            session.webSocket().writeTextMessage(error);
            return;
        }

        // Validate schema
        var validationResult = schemaValidator.validate(session.protocol(), call.action(), call.payload());
        if (!validationResult.isValid()) {
            log.warn("Schema validation failed for {} from {}: {}",
                    call.action(), session.stationIdentity().value(), validationResult.errorMessage());
            String error = codec.buildCallError(call.messageId(),
                    validationResult.errorCode(), validationResult.errorMessage());
            log.debug("OCPP OUT [{}] {}", session.stationIdentity().value(), error);
            session.webSocket().writeTextMessage(error);
            return;
        }

        // Dispatch to handler
        Optional<OcppMessageHandler> handler = dispatcher.getHandler(session.protocol(), call.action());
        if (handler.isEmpty()) {
            log.warn("No handler for action {} (protocol {})", call.action(), session.protocol());
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.NOT_IMPLEMENTED, "Action not supported: " + call.action());
            log.debug("OCPP OUT [{}] {}", session.stationIdentity().value(), error);
            session.webSocket().writeTextMessage(error);
            return;
        }

        try {
            OcppMessageHandler h = handler.get();
            JsonNode responsePayload = h.handle(session, call.messageId(), call.payload());

            // Self-check: the payload we're about to send to the station must itself
            // conform to the response schema. Warn-only for now since response schemas
            // are still being backfilled.
            var responseCheck = schemaValidator.validateResponse(session.protocol(), call.action(), responsePayload);
            if (!responseCheck.isValid()) {
                log.warn("Our {} response to {} failed schema validation: {}",
                        call.action(), session.stationIdentity().value(), responseCheck.errorMessage());
            }

            String response = codec.buildCallResult(call.messageId(), responsePayload);
            log.debug("OCPP OUT [{}] {}", session.stationIdentity().value(), response);
            session.webSocket().writeTextMessage(response);
            h.afterResponse(session);
        } catch (Exception e) {
            log.error("Error handling {} from {}: {}", call.action(), session.stationIdentity().value(), e.getMessage(), e);
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.INTERNAL_ERROR, "Internal error processing " + call.action());
            log.debug("OCPP OUT [{}] {}", session.stationIdentity().value(), error);
            session.webSocket().writeTextMessage(error);
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
}
