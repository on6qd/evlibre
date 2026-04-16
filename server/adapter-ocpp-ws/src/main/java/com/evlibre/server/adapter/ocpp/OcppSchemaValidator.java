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

public class OcppSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(OcppSchemaValidator.class);

    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public ValidationResult validate(OcppProtocol protocol, String action, JsonNode payload) {
        JsonSchema schema = getSchema(protocol, action);
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

        // OCPP 1.6 RPC Framework error codes:
        //   - ProtocolError: payload is incomplete (missing required field)
        //   - PropertyConstraintViolation: value does not meet the constraint for its type
        //   - FormationViolation: payload does not conform to the PDU structure
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

    private JsonSchema getSchema(OcppProtocol protocol, String action) {
        String key = protocol.name() + ":" + action;
        return schemaCache.computeIfAbsent(key, k -> loadSchema(protocol, action));
    }

    private JsonSchema loadSchema(OcppProtocol protocol, String action) {
        String path = schemaPath(protocol, action);
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            log.debug("No schema found for {} {}", protocol, action);
            return null;
        }

        try {
            SpecVersion.VersionFlag version = (protocol == OcppProtocol.OCPP_16)
                    ? SpecVersion.VersionFlag.V4
                    : SpecVersion.VersionFlag.V6;

            // Parse the schema JSON ourselves so we can pass a JsonNode
            ObjectMapper mapper = new ObjectMapper();
            JsonNode schemaNode = mapper.readTree(is);

            // Remove id/$id to avoid URI parsing issues with OCPP URN format
            var objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) schemaNode;
            objectNode.remove("$id");
            objectNode.remove("id");

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
            return factory.getSchema(schemaNode);
        } catch (Exception e) {
            log.warn("Failed to load schema for {} {}: {}", protocol, action, e.getMessage());
            return null;
        }
    }

    private String schemaPath(OcppProtocol protocol, String action) {
        if (protocol == OcppProtocol.OCPP_16) {
            return "schemas/ocpp16/" + action + ".json";
        } else {
            return "schemas/ocpp201/" + action + "Request.json";
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
