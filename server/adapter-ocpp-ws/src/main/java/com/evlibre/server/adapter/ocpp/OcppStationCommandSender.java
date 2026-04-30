package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceEventPublisher;
import com.evlibre.server.core.domain.shared.ports.outbound.MessageTraceStorePort;
import com.evlibre.server.core.domain.v16.ports.outbound.Ocpp16StationCommandSender;
import com.evlibre.server.core.domain.v201.ports.outbound.Ocpp201StationCommandSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sends CSMS → CS calls over the WebSocket transport. Exposes two typed ports —
 * {@link #v16()} and {@link #v201()} — so v1.6 and v2.0.1 use cases each depend
 * only on their own protocol's sender type. Each port rejects the call when the
 * session's negotiated protocol doesn't match.
 */
public class OcppStationCommandSender {

    private static final Logger log = LoggerFactory.getLogger(OcppStationCommandSender.class);

    private static final MessageTraceStorePort NOOP_TRACE_STORE = new MessageTraceStorePort() {
        @Override public void record(TenantId t, ChargePointIdentity s, MessageTraceEntry e) {}
        @Override public List<MessageTraceEntry> recent(TenantId t, ChargePointIdentity s) { return List.of(); }
    };
    private static final MessageTraceEventPublisher NOOP_TRACE_EVENTS = (t, s, e) -> {};

    private final OcppSessionManager sessionManager;
    private final OcppMessageCodec codec;
    private final OcppPendingCallManager pendingCallManager;
    private final ObjectMapper objectMapper;
    private final OcppSchemaValidator schemaValidator;
    private final MessageTraceStorePort traceStore;
    private final MessageTraceEventPublisher traceEvents;

    private final Ocpp16StationCommandSender v16Port = this::send16;
    private final Ocpp201StationCommandSender v201Port = this::send201;

    public OcppStationCommandSender(OcppSessionManager sessionManager,
                                     OcppMessageCodec codec,
                                     OcppPendingCallManager pendingCallManager,
                                     ObjectMapper objectMapper) {
        this(sessionManager, codec, pendingCallManager, objectMapper, null,
                NOOP_TRACE_STORE, NOOP_TRACE_EVENTS);
    }

    public OcppStationCommandSender(OcppSessionManager sessionManager,
                                     OcppMessageCodec codec,
                                     OcppPendingCallManager pendingCallManager,
                                     ObjectMapper objectMapper,
                                     OcppSchemaValidator schemaValidator) {
        this(sessionManager, codec, pendingCallManager, objectMapper, schemaValidator,
                NOOP_TRACE_STORE, NOOP_TRACE_EVENTS);
    }

    public OcppStationCommandSender(OcppSessionManager sessionManager,
                                     OcppMessageCodec codec,
                                     OcppPendingCallManager pendingCallManager,
                                     ObjectMapper objectMapper,
                                     OcppSchemaValidator schemaValidator,
                                     MessageTraceStorePort traceStore,
                                     MessageTraceEventPublisher traceEvents) {
        this.sessionManager = sessionManager;
        this.codec = codec;
        this.pendingCallManager = pendingCallManager;
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.traceStore = traceStore;
        this.traceEvents = traceEvents;
    }

    public Ocpp16StationCommandSender v16() {
        return v16Port;
    }

    public Ocpp201StationCommandSender v201() {
        return v201Port;
    }

    private CompletableFuture<Map<String, Object>> send16(TenantId tenantId,
                                                           ChargePointIdentity stationIdentity,
                                                           String action,
                                                           Map<String, Object> payload) {
        return sendEnforced(OcppProtocol.OCPP_16, tenantId, stationIdentity, action, payload);
    }

    private CompletableFuture<Map<String, Object>> send201(TenantId tenantId,
                                                            ChargePointIdentity stationIdentity,
                                                            String action,
                                                            Map<String, Object> payload) {
        return sendEnforced(OcppProtocol.OCPP_201, tenantId, stationIdentity, action, payload);
    }

    private CompletableFuture<Map<String, Object>> sendEnforced(OcppProtocol requiredProtocol,
                                                                 TenantId tenantId,
                                                                 ChargePointIdentity stationIdentity,
                                                                 String action,
                                                                 Map<String, Object> payload) {
        var maybeSession = sessionManager.getSession(tenantId, stationIdentity);
        if (maybeSession.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Station not connected: " + stationIdentity.value()));
        }
        OcppSession session = maybeSession.get();
        if (session.protocol() != requiredProtocol) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Cannot send " + action + " via " + requiredProtocol
                            + ": station " + stationIdentity.value()
                            + " is connected with " + session.protocol()));
        }

        JsonNode payloadNode = objectMapper.valueToTree(payload);

        // Reject any outbound CALL whose payload doesn't match the request schema.
        // Missing schema still passes (see OcppSchemaValidator contract) so non-wired
        // actions aren't blocked.
        if (schemaValidator != null) {
            var outboundCheck = schemaValidator.validateRequest(requiredProtocol, action, payloadNode);
            if (!outboundCheck.isValid()) {
                log.warn("Outbound {} ({}) payload failed schema validation: {}",
                        action, requiredProtocol, outboundCheck.errorMessage());
                return CompletableFuture.failedFuture(new IllegalArgumentException(
                        "Outbound " + action + " payload failed schema validation: "
                                + outboundCheck.errorMessage()));
            }
        }

        var pendingCall = pendingCallManager.createPendingCall(action, requiredProtocol);
        String message = codec.buildCall(pendingCall.messageId(), action, payloadNode);

        log.info("Sending {} to {} (msgId: {})", action, stationIdentity.value(), pendingCall.messageId());
        session.webSocket().writeTextMessage(message);
        MessageTraceEntry.OcppFrame entry = new MessageTraceEntry.OcppFrame(
                Instant.now(), Direction.OUT, FrameType.CALL, action, pendingCall.messageId(), message);
        traceStore.record(tenantId, stationIdentity, entry);
        traceEvents.messageRecorded(tenantId, stationIdentity, entry);

        @SuppressWarnings("unchecked")
        CompletableFuture<Map<String, Object>> result = pendingCall.future().thenApply(jsonNode ->
                (Map<String, Object>) objectMapper.convertValue(jsonNode, Map.class));
        return result;
    }
}
