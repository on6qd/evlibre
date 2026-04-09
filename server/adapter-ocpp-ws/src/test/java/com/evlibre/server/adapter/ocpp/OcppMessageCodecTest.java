package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.MessageTypeId;
import com.evlibre.common.ocpp.OcppErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcppMessageCodecTest {

    private OcppMessageCodec codec;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        codec = new OcppMessageCodec(mapper);
    }

    @Test
    void parse_call() throws Exception {
        String json = "[2,\"msg-1\",\"BootNotification\",{\"chargePointVendor\":\"ABB\"}]";
        var parsed = codec.parse(json);

        assertThat(parsed.typeId()).isEqualTo(MessageTypeId.CALL);
        var call = (OcppCallMessage) parsed.message();
        assertThat(call.messageId()).isEqualTo("msg-1");
        assertThat(call.action()).isEqualTo("BootNotification");
        assertThat(call.payload().get("chargePointVendor").asText()).isEqualTo("ABB");
    }

    @Test
    void parse_callResult() throws Exception {
        String json = "[3,\"msg-1\",{\"status\":\"Accepted\"}]";
        var parsed = codec.parse(json);

        assertThat(parsed.typeId()).isEqualTo(MessageTypeId.CALL_RESULT);
        var result = (OcppCallResultMessage) parsed.message();
        assertThat(result.messageId()).isEqualTo("msg-1");
        assertThat(result.payload().get("status").asText()).isEqualTo("Accepted");
    }

    @Test
    void parse_callError() throws Exception {
        String json = "[4,\"msg-1\",\"NotImplemented\",\"Action not supported\",{}]";
        var parsed = codec.parse(json);

        assertThat(parsed.typeId()).isEqualTo(MessageTypeId.CALL_ERROR);
        var error = (OcppCallErrorMessage) parsed.message();
        assertThat(error.messageId()).isEqualTo("msg-1");
        assertThat(error.errorDescription()).isEqualTo("Action not supported");
    }

    @Test
    void parse_invalidJson_throws() {
        assertThatThrownBy(() -> codec.parse("not json"))
                .isInstanceOf(OcppMessageCodec.OcppMessageParseException.class)
                .hasMessageContaining("Invalid JSON");
    }

    @Test
    void parse_notArray_throws() {
        assertThatThrownBy(() -> codec.parse("{\"key\":\"value\"}"))
                .isInstanceOf(OcppMessageCodec.OcppMessageParseException.class)
                .hasMessageContaining("JSON array");
    }

    @Test
    void parse_tooShort_throws() {
        assertThatThrownBy(() -> codec.parse("[2,\"msg-1\"]"))
                .isInstanceOf(OcppMessageCodec.OcppMessageParseException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void parse_unknownTypeId_throws() {
        assertThatThrownBy(() -> codec.parse("[9,\"msg-1\",\"Action\",{}]"))
                .isInstanceOf(OcppMessageCodec.OcppMessageParseException.class)
                .hasMessageContaining("Unknown message type");
    }

    @Test
    void parse_call_wrongSize_throws() {
        assertThatThrownBy(() -> codec.parse("[2,\"msg-1\",\"Action\"]"))
                .isInstanceOf(OcppMessageCodec.OcppMessageParseException.class)
                .hasMessageContaining("4 elements");
    }

    @Test
    void buildCallResult() {
        var payload = mapper.createObjectNode().put("status", "Accepted");
        String result = codec.buildCallResult("msg-1", payload);

        assertThat(result).isEqualTo("[3,\"msg-1\",{\"status\":\"Accepted\"}]");
    }

    @Test
    void buildCallError() {
        String result = codec.buildCallError("msg-1", OcppErrorCode.NOT_IMPLEMENTED, "Not supported");

        assertThat(result).contains("\"NotImplemented\"");
        assertThat(result).contains("\"Not supported\"");
        assertThat(result).startsWith("[4,");
    }

    @Test
    void buildCall() {
        var payload = mapper.createObjectNode().put("type", "Soft");
        String result = codec.buildCall("msg-2", "Reset", payload);

        assertThat(result).isEqualTo("[2,\"msg-2\",\"Reset\",{\"type\":\"Soft\"}]");
    }
}
