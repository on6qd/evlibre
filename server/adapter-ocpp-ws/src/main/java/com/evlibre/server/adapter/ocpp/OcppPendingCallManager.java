package com.evlibre.server.adapter.ocpp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OcppPendingCallManager {

    private static final Logger log = LoggerFactory.getLogger(OcppPendingCallManager.class);
    private static final long TIMEOUT_SECONDS = 30;

    private final Map<String, CompletableFuture<JsonNode>> pendingCalls = new ConcurrentHashMap<>();

    public record PendingCall(String messageId, CompletableFuture<JsonNode> future) {}

    public PendingCall createPendingCall() {
        String messageId = UUID.randomUUID().toString();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, error) -> pendingCalls.remove(messageId));

        pendingCalls.put(messageId, future);
        return new PendingCall(messageId, future);
    }

    public void resolveCallResult(String messageId, JsonNode payload) {
        CompletableFuture<JsonNode> future = pendingCalls.remove(messageId);
        if (future != null) {
            future.complete(payload);
        } else {
            log.warn("No pending call for messageId: {}", messageId);
        }
    }

    public void resolveCallError(String messageId, String errorCode, String errorDescription) {
        CompletableFuture<JsonNode> future = pendingCalls.remove(messageId);
        if (future != null) {
            future.completeExceptionally(
                    new RuntimeException("OCPP error " + errorCode + ": " + errorDescription));
        }
    }
}
