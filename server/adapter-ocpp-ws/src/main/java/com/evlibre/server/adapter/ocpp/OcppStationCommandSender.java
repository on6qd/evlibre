package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.outbound.StationCommandSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OcppStationCommandSender implements StationCommandSender {

    private static final Logger log = LoggerFactory.getLogger(OcppStationCommandSender.class);

    private final OcppSessionManager sessionManager;
    private final OcppMessageCodec codec;
    private final OcppPendingCallManager pendingCallManager;
    private final ObjectMapper objectMapper;

    public OcppStationCommandSender(OcppSessionManager sessionManager,
                                     OcppMessageCodec codec,
                                     OcppPendingCallManager pendingCallManager,
                                     ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.codec = codec;
        this.pendingCallManager = pendingCallManager;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> sendCommand(TenantId tenantId, ChargePointIdentity stationIdentity,
                                                               String action, Map<String, Object> payload) {
        var session = sessionManager.getSession(tenantId, stationIdentity);
        if (session.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Station not connected: " + stationIdentity.value()));
        }

        JsonNode payloadNode = objectMapper.valueToTree(payload);
        var pendingCall = pendingCallManager.createPendingCall();
        String message = codec.buildCall(pendingCall.messageId(), action, payloadNode);

        log.info("Sending {} to {} (msgId: {})", action, stationIdentity.value(), pendingCall.messageId());
        session.get().webSocket().writeTextMessage(message);

        return pendingCall.future().thenApply(jsonNode ->
                objectMapper.convertValue(jsonNode, Map.class));
    }
}
