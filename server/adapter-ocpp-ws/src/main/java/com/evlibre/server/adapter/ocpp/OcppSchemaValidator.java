package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppErrorCode;
import com.evlibre.common.ocpp.OcppProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Validates OCPP CALL and CALL_RESULT payloads against JSON schemas on the
 * classpath. Schema lookup uses {@code schemas/{protocol}/{Action}[Request|Response].json}
 * — inbound requests validate against {@code Request}, responses against {@code Response}.
 *
 * <p>When a schema is missing the validator returns {@link ValidationResult#valid()}
 * so partial coverage is non-blocking; fill gaps by dropping schema files next to the
 * existing ones.
 */
public class OcppSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(OcppSchemaValidator.class);

    private enum Direction {
        REQUEST, RESPONSE
    }

    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    /**
     * Validates an inbound CALL payload (station → CSMS request, or a CSMS-initiated
     * request about to go out). Kept as the primary entry point for backward compat.
     */
    public ValidationResult validate(OcppProtocol protocol, String action, JsonNode payload) {
        return validateAgainst(protocol, action, payload, Direction.REQUEST);
    }

    /**
     * Validates a request payload. Same as {@link #validate} but named clearly.
     */
    public ValidationResult validateRequest(OcppProtocol protocol, String action, JsonNode payload) {
        return validateAgainst(protocol, action, payload, Direction.REQUEST);
    }

    /**
     * Validates a response (CALL_RESULT) payload — either our own outbound response
     * to the station, or the station's response to our outbound CALL.
     */
    public ValidationResult validateResponse(OcppProtocol protocol, String action, JsonNode payload) {
        return validateAgainst(protocol, action, payload, Direction.RESPONSE);
    }

    private ValidationResult validateAgainst(OcppProtocol protocol, String action,
                                              JsonNode payload, Direction direction) {
        JsonSchema schema = getSchema(protocol, action, direction);
        if (schema == null) {
            return ValidationResult.valid();
        }

        Set<ValidationMessage> errors = schema.validate(payload);
        if (errors.isEmpty()) {
            return ValidationResult.valid();
        }

        String errorMessage = errors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.joining("; "));

        OcppErrorCode errorCode = classify(errors);
        return new ValidationResult(false, errorMessage, errorCode);
    }

    private static OcppErrorCode classify(Set<ValidationMessage> errors) {
        boolean hasMissingRequired = errors.stream().anyMatch(vm ->
                "required".equals(vm.getType()) || vm.getMessage().contains("required"));
        if (hasMissingRequired) {
            return OcppErrorCode.PROTOCOL_ERROR;
        }
        boolean hasConstraintViolation = errors.stream().anyMatch(vm -> {
            String t = vm.getType();
            return "maxLength".equals(t) || "minLength".equals(t) || "enum".equals(t)
                    || "pattern".equals(t) || "maximum".equals(t) || "minimum".equals(t);
        });
        if (hasConstraintViolation) {
            return OcppErrorCode.PROPERTY_CONSTRAINT_VIOLATION;
        }
        return OcppErrorCode.FORMATION_VIOLATION;
    }

    private JsonSchema getSchema(OcppProtocol protocol, String action, Direction direction) {
        String key = protocol.name() + ":" + action + ":" + direction;
        return schemaCache.computeIfAbsent(key, k -> loadSchema(protocol, action, direction));
    }

    private JsonSchema loadSchema(OcppProtocol protocol, String action, Direction direction) {
        String path = schemaPath(protocol, action, direction);
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            log.debug("No schema found for {} {} {}", protocol, action, direction);
            return null;
        }

        try {
            SpecVersion.VersionFlag version = (protocol == OcppProtocol.OCPP_16)
                    ? SpecVersion.VersionFlag.V4
                    : SpecVersion.VersionFlag.V6;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode schemaNode = mapper.readTree(is);

            // Remove id/$id to avoid URI parsing issues with OCPP URN format
            var objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) schemaNode;
            objectNode.remove("$id");
            objectNode.remove("id");

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
            return factory.getSchema(schemaNode);
        } catch (Exception e) {
            log.warn("Failed to load schema for {} {} {}: {}", protocol, action, direction, e.getMessage());
            return null;
        }
    }

    private String schemaPath(OcppProtocol protocol, String action, Direction direction) {
        // v1.6 convention today: <Action>.json = request; <Action>Response.json = response.
        // v2.0.1 convention: <Action>Request.json = request; <Action>Response.json = response.
        if (protocol == OcppProtocol.OCPP_16) {
            String suffix = direction == Direction.RESPONSE ? "Response" : "";
            return "schemas/ocpp16/" + action + suffix + ".json";
        } else {
            String suffix = direction == Direction.RESPONSE ? "Response" : "Request";
            return "schemas/ocpp201/" + action + suffix + ".json";
        }
    }

    public record ValidationResult(boolean isValid, String errorMessage, OcppErrorCode errorCode) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, OcppErrorCode.FORMATION_VIOLATION);
        }
    }
}
