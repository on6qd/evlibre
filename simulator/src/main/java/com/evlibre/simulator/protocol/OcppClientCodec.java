package com.evlibre.simulator.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.UUID;

public class OcppClientCodec {

    private final ObjectMapper objectMapper;

    public OcppClientCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildCall(String action, JsonNode payload) {
        return buildCall(UUID.randomUUID().toString(), action, payload);
    }

    public String buildCall(String messageId, String action, JsonNode payload) {
        ArrayNode frame = objectMapper.createArrayNode();
        frame.add(2); // CALL
        frame.add(messageId);
        frame.add(action);
        frame.add(payload);
        return frame.toString();
    }

    public String buildCallResult(String messageId, JsonNode payload) {
        ArrayNode frame = objectMapper.createArrayNode();
        frame.add(3); // CALLRESULT
        frame.add(messageId);
        frame.add(payload);
        return frame.toString();
    }

    public ParsedMessage parse(String raw) throws Exception {
        JsonNode frame = objectMapper.readTree(raw);
        if (!frame.isArray() || frame.size() < 3) {
            throw new IllegalArgumentException("Invalid OCPP frame: " + raw);
        }

        int typeId = frame.get(0).asInt();
        String messageId = frame.get(1).asText();

        return switch (typeId) {
            case 2 -> // CALL (server command)
                    new ParsedMessage(ParsedMessage.Type.CALL, messageId, frame.get(2).asText(), frame.get(3));
            case 3 -> // CALLRESULT
                    new ParsedMessage(ParsedMessage.Type.CALL_RESULT, messageId, null, frame.get(2));
            case 4 -> // CALLERROR
                    new ParsedMessage(ParsedMessage.Type.CALL_ERROR, messageId, frame.get(2).asText(), frame.size() > 4 ? frame.get(4) : null);
            default -> throw new IllegalArgumentException("Unknown message type: " + typeId);
        };
    }

    public record ParsedMessage(Type type, String messageId, String action, JsonNode payload) {
        public enum Type { CALL, CALL_RESULT, CALL_ERROR }
    }
}
