package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.MessageTypeId;
import com.evlibre.common.ocpp.OcppErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class OcppMessageCodec {

    private final ObjectMapper objectMapper;

    public OcppMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ParsedMessage(MessageTypeId typeId, Object message) {}

    public ParsedMessage parse(String raw) throws OcppMessageParseException {
        JsonNode node;
        try {
            node = objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new OcppMessageParseException("Invalid JSON: " + e.getMessage());
        }

        if (!node.isArray()) {
            throw new OcppMessageParseException("OCPP message must be a JSON array");
        }

        ArrayNode array = (ArrayNode) node;
        if (array.size() < 3) {
            throw new OcppMessageParseException("OCPP message array too short: " + array.size());
        }

        int typeIdValue = array.get(0).asInt();
        MessageTypeId typeId = MessageTypeId.fromValue(typeIdValue);
        if (typeId == null) {
            throw new OcppMessageParseException("Unknown message type ID: " + typeIdValue);
        }

        return switch (typeId) {
            case CALL -> parseCall(array);
            case CALL_RESULT -> parseCallResult(array);
            case CALL_ERROR -> parseCallError(array);
        };
    }

    private ParsedMessage parseCall(ArrayNode array) throws OcppMessageParseException {
        if (array.size() != 4) {
            throw new OcppMessageParseException("CALL message must have 4 elements, got " + array.size());
        }
        String messageId = array.get(1).asText();
        String action = array.get(2).asText();
        JsonNode payload = array.get(3);
        return new ParsedMessage(MessageTypeId.CALL, new OcppCallMessage(messageId, action, payload));
    }

    private ParsedMessage parseCallResult(ArrayNode array) throws OcppMessageParseException {
        if (array.size() != 3) {
            throw new OcppMessageParseException("CALLRESULT message must have 3 elements, got " + array.size());
        }
        String messageId = array.get(1).asText();
        JsonNode payload = array.get(2);
        return new ParsedMessage(MessageTypeId.CALL_RESULT, new OcppCallResultMessage(messageId, payload));
    }

    private ParsedMessage parseCallError(ArrayNode array) throws OcppMessageParseException {
        if (array.size() < 4) {
            throw new OcppMessageParseException("CALLERROR message must have at least 4 elements, got " + array.size());
        }
        String messageId = array.get(1).asText();
        String errorCodeStr = array.get(2).asText();
        String errorDescription = array.get(3).asText();
        String errorDetails = array.size() > 4 ? array.get(4).toString() : "{}";

        OcppErrorCode errorCode;
        try {
            errorCode = OcppErrorCode.valueOf(errorCodeStr.replace(".", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            errorCode = OcppErrorCode.GENERIC_ERROR;
        }

        return new ParsedMessage(MessageTypeId.CALL_ERROR,
                new OcppCallErrorMessage(messageId, errorCode, errorDescription, errorDetails));
    }

    public String buildCallResult(String messageId, JsonNode payload) {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(MessageTypeId.CALL_RESULT.value());
        array.add(messageId);
        array.add(payload);
        return array.toString();
    }

    public String buildCallError(String messageId, OcppErrorCode errorCode,
                                  String errorDescription) {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(MessageTypeId.CALL_ERROR.value());
        array.add(messageId);
        array.add(errorCode.value());
        array.add(errorDescription);
        array.add(objectMapper.createObjectNode());
        return array.toString();
    }

    public String buildCall(String messageId, String action, JsonNode payload) {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(MessageTypeId.CALL.value());
        array.add(messageId);
        array.add(action);
        array.add(payload);
        return array.toString();
    }

    public static class OcppMessageParseException extends Exception {
        public OcppMessageParseException(String message) {
            super(message);
        }
    }
}
