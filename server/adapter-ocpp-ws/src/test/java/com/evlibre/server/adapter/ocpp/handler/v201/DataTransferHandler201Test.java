package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleDataTransferPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DataTransferHandler201Test {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CS-201");

    private AtomicReference<DataTransferResult> nextResult;
    private DataTransferHandler201 handler;
    private OcppSession session;

    @BeforeEach
    void setUp() {
        nextResult = new AtomicReference<>(DataTransferResult.of(DataTransferStatus.ACCEPTED));
        HandleDataTransferPort port = (tid, vendor, msg, data) -> nextResult.get();
        handler = new DataTransferHandler201(port, objectMapper);
        session = new OcppSession(tenantId, stationIdentity, OcppProtocol.OCPP_201, null);
    }

    @Test
    void minimal_accepted_response_emits_only_status() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"vendorId":"com.evlibre.probe"}""");

        JsonNode response = handler.handle(session, "m-1", payload);

        assertThat(response.get("status").asText()).isEqualTo("Accepted");
        assertThat(response.has("data")).isFalse();
        assertThat(response.has("statusInfo")).isFalse();
    }

    // Response data is anyType per the 2.0.1 schema — objects, lists, and primitives
    // all have to round-trip back onto the wire so a vendor-scoped handler can echo
    // structured payloads in the response.
    @Test
    void object_data_is_serialized_on_response() throws Exception {
        nextResult.set(new DataTransferResult(DataTransferStatus.ACCEPTED,
                Map.of("echoed", 42), null));
        JsonNode payload = objectMapper.readTree("""
                {"vendorId":"com.evlibre.probe"}""");

        JsonNode response = handler.handle(session, "m-2", payload);

        assertThat(response.get("status").asText()).isEqualTo("Accepted");
        assertThat(response.get("data").get("echoed").asInt()).isEqualTo(42);
    }

    @Test
    void statusInfoReason_is_emitted_as_statusInfo_object() throws Exception {
        nextResult.set(new DataTransferResult(DataTransferStatus.REJECTED, null, "VendorError"));
        JsonNode payload = objectMapper.readTree("""
                {"vendorId":"com.evlibre.probe"}""");

        JsonNode response = handler.handle(session, "m-3", payload);

        assertThat(response.get("status").asText()).isEqualTo("Rejected");
        assertThat(response.get("statusInfo").get("reasonCode").asText()).isEqualTo("VendorError");
    }
}
