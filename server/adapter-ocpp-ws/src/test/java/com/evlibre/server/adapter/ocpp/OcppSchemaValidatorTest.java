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
}
