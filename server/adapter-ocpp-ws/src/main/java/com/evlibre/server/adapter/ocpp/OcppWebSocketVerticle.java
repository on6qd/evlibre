package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.MessageTypeId;
import com.evlibre.common.ocpp.OcppErrorCode;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;
import com.evlibre.server.core.domain.model.TenantId;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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
    private final OcppMessageCodec codec;
    private final OcppSchemaValidator schemaValidator;
    private final OcppMessageDispatcher dispatcher;
    private final OcppSessionManager sessionManager;
    private final OcppProtocolNegotiator protocolNegotiator;
    private final OcppPendingCallManager pendingCallManager;
    private HttpServer httpServer;

    public OcppWebSocketVerticle(int port,
                                  OcppMessageCodec codec,
                                  OcppSchemaValidator schemaValidator,
                                  OcppMessageDispatcher dispatcher,
                                  OcppSessionManager sessionManager,
                                  OcppProtocolNegotiator protocolNegotiator,
                                  OcppPendingCallManager pendingCallManager) {
        this.port = port;
        this.codec = codec;
        this.schemaValidator = schemaValidator;
        this.dispatcher = dispatcher;
        this.sessionManager = sessionManager;
        this.protocolNegotiator = protocolNegotiator;
        this.pendingCallManager = pendingCallManager;
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
            ctx.request().toWebSocket().onSuccess(ws -> {
                handleWebSocketConnection(ws, ctx.pathParam("tenantId"), ctx.pathParam("stationId"));
            }).onFailure(err -> {
                log.error("WebSocket upgrade failed: {}", err.getMessage());
            });
        });

        httpServer.requestHandler(router).listen(port)
                .onSuccess(server -> {
                    log.info("OCPP WebSocket server listening on port {}", server.actualPort());
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

        OcppSession session = new OcppSession(tenantId, stationIdentity, protocol, ws);
        sessionManager.register(session);

        ws.textMessageHandler(message -> handleMessage(session, message));

        ws.closeHandler(v -> sessionManager.unregister(tenantId, stationIdentity));

        ws.exceptionHandler(err ->
                log.error("WebSocket error for station {}: {}", stationIdStr, err.getMessage()));
    }

    private void handleMessage(OcppSession session, String rawMessage) {
        OcppMessageCodec.ParsedMessage parsed;
        try {
            parsed = codec.parse(rawMessage);
        } catch (OcppMessageCodec.OcppMessageParseException e) {
            log.warn("Failed to parse OCPP message from {}: {}", session.stationIdentity().value(), e.getMessage());
            return;
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
        // Validate schema
        var validationResult = schemaValidator.validate(session.protocol(), call.action(), call.payload());
        if (!validationResult.isValid()) {
            log.warn("Schema validation failed for {} from {}: {}",
                    call.action(), session.stationIdentity().value(), validationResult.errorMessage());
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.FORMATION_VIOLATION, validationResult.errorMessage());
            session.webSocket().writeTextMessage(error);
            return;
        }

        // Dispatch to handler
        Optional<OcppMessageHandler> handler = dispatcher.getHandler(session.protocol(), call.action());
        if (handler.isEmpty()) {
            log.warn("No handler for action {} (protocol {})", call.action(), session.protocol());
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.NOT_IMPLEMENTED, "Action not supported: " + call.action());
            session.webSocket().writeTextMessage(error);
            return;
        }

        try {
            JsonNode responsePayload = handler.get().handle(session, call.messageId(), call.payload());
            String response = codec.buildCallResult(call.messageId(), responsePayload);
            session.webSocket().writeTextMessage(response);
        } catch (Exception e) {
            log.error("Error handling {} from {}: {}", call.action(), session.stationIdentity().value(), e.getMessage(), e);
            String error = codec.buildCallError(call.messageId(),
                    OcppErrorCode.INTERNAL_ERROR, "Internal error processing " + call.action());
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
