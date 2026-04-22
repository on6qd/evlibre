package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcppSchemaValidatorTest {

    private OcppSchemaValidator validator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        validator = new OcppSchemaValidator();
        mapper = new ObjectMapper();
    }

    @Test
    void ocpp16_bootNotification_valid() throws Exception {
        var payload = mapper.readTree(
                "{\"chargePointVendor\":\"ABB\",\"chargePointModel\":\"Terra AC\"}");

        var result = validator.validate(OcppProtocol.OCPP_16, "BootNotification", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp16_bootNotification_missingRequired() throws Exception {
        var payload = mapper.readTree("{\"chargePointVendor\":\"ABB\"}");

        var result = validator.validate(OcppProtocol.OCPP_16, "BootNotification", payload);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("chargePointModel");
    }

    @Test
    void ocpp201_bootNotification_valid() throws Exception {
        var payload = mapper.readTree(
                "{\"chargingStation\":{\"vendorName\":\"ABB\",\"model\":\"Terra AC\"},\"reason\":\"PowerUp\"}");

        var result = validator.validate(OcppProtocol.OCPP_201, "BootNotification", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp201_bootNotification_missingRequired() throws Exception {
        var payload = mapper.readTree("{\"reason\":\"PowerUp\"}");

        var result = validator.validate(OcppProtocol.OCPP_201, "BootNotification", payload);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("chargingStation");
    }

    @Test
    void unknownAction_passesValidation() throws Exception {
        var payload = mapper.readTree("{\"anything\":true}");

        var result = validator.validate(OcppProtocol.OCPP_16, "UnknownAction", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp201_notifyReport_request_valid() throws Exception {
        // Minimal NotifyReport that has the three required top-level fields.
        var payload = mapper.readTree(
                "{\"requestId\":1,\"generatedAt\":\"2026-04-21T10:00:00Z\",\"seqNo\":0,\"reportData\":[]}");

        var result = validator.validateRequest(OcppProtocol.OCPP_201, "NotifyReport", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp201_notifyReport_request_missingRequired() throws Exception {
        var payload = mapper.readTree("{\"requestId\":1}");

        var result = validator.validateRequest(OcppProtocol.OCPP_201, "NotifyReport", payload);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("generatedAt");
    }

    @Test
    void validateResponse_missingSchema_passes() throws Exception {
        // "No schema on classpath = valid" is the validator's contract so gaps in
        // coverage never block the server; use an action we deliberately don't
        // author a schema for.
        var payload = mapper.readTree("{\"status\":\"Accepted\"}");

        var result = validator.validateResponse(OcppProtocol.OCPP_201, "UnknownAction", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp201_bootNotification_response_valid() throws Exception {
        var payload = mapper.readTree(
                "{\"currentTime\":\"2026-04-21T10:00:00Z\",\"interval\":900,\"status\":\"Accepted\"}");

        var result = validator.validateResponse(OcppProtocol.OCPP_201, "BootNotification", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp201_bootNotification_response_missingRequired() throws Exception {
        var payload = mapper.readTree("{\"status\":\"Accepted\"}");

        var result = validator.validateResponse(OcppProtocol.OCPP_201, "BootNotification", payload);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void ocpp16_authorize_response_valid() throws Exception {
        var payload = mapper.readTree(
                "{\"idTagInfo\":{\"status\":\"Accepted\"}}");

        var result = validator.validateResponse(OcppProtocol.OCPP_16, "Authorize", payload);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ocpp16_authorize_response_invalidStatus() throws Exception {
        var payload = mapper.readTree(
                "{\"idTagInfo\":{\"status\":\"NotARealStatus\"}}");

        var result = validator.validateResponse(OcppProtocol.OCPP_16, "Authorize", payload);

        assertThat(result.isValid()).isFalse();
    }
}
