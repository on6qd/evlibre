package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;
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

    private final Map<String, PendingEntry> pendingCalls = new ConcurrentHashMap<>();
    private final OcppSchemaValidator schemaValidator;

    public OcppPendingCallManager() {
        this(null);
    }

    public OcppPendingCallManager(OcppSchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    public record PendingCall(String messageId, CompletableFuture<JsonNode> future) {}

    private record PendingEntry(CompletableFuture<JsonNode> future, String action, OcppProtocol protocol) {}

    /**
     * Create a pending call without schema-aware validation of the eventual response.
     * Prefer the {@link #createPendingCall(String, OcppProtocol)} overload whenever the
     * action and negotiated protocol are known — it enables response validation.
     */
    public PendingCall createPendingCall() {
        return createPendingCall(null, null);
    }

    public PendingCall createPendingCall(String action, OcppProtocol protocol) {
        String messageId = UUID.randomUUID().toString();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, error) -> pendingCalls.remove(messageId));

        pendingCalls.put(messageId, new PendingEntry(future, action, protocol));
        return new PendingCall(messageId, future);
    }

    public void resolveCallResult(String messageId, JsonNode payload) {
        PendingEntry entry = pendingCalls.remove(messageId);
        if (entry == null) {
            log.warn("No pending call for messageId: {}", messageId);
            return;
        }
        // Validate the station's response against the expected schema if we know what
        // to expect. Validation failure is logged but the future still completes — a
        // partial schema library shouldn't crash callers that got a plausibly-correct
        // response.
        if (schemaValidator != null && entry.action() != null && entry.protocol() != null) {
            var result = schemaValidator.validateResponse(entry.protocol(), entry.action(), payload);
            if (!result.isValid()) {
                log.warn("Station response to {} ({}) failed schema validation: {}",
                        entry.action(), entry.protocol(), result.errorMessage());
            }
        }
        entry.future().complete(payload);
    }

    public void resolveCallError(String messageId, String errorCode, String errorDescription) {
        PendingEntry entry = pendingCalls.remove(messageId);
        if (entry != null) {
            entry.future().completeExceptionally(
                    new RuntimeException("OCPP error " + errorCode + ": " + errorDescription));
        }
    }
}
