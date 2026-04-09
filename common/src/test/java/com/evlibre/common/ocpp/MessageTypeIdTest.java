package com.evlibre.common.ocpp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeIdTest {

    @Test
    void values() {
        assertThat(MessageTypeId.CALL.value()).isEqualTo(2);
        assertThat(MessageTypeId.CALL_RESULT.value()).isEqualTo(3);
        assertThat(MessageTypeId.CALL_ERROR.value()).isEqualTo(4);
    }

    @Test
    void fromValue_valid() {
        assertThat(MessageTypeId.fromValue(2)).isEqualTo(MessageTypeId.CALL);
        assertThat(MessageTypeId.fromValue(3)).isEqualTo(MessageTypeId.CALL_RESULT);
        assertThat(MessageTypeId.fromValue(4)).isEqualTo(MessageTypeId.CALL_ERROR);
    }

    @Test
    void fromValue_unknown_returnsNull() {
        assertThat(MessageTypeId.fromValue(0)).isNull();
        assertThat(MessageTypeId.fromValue(5)).isNull();
    }
}
