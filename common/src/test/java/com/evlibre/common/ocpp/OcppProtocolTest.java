package com.evlibre.common.ocpp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcppProtocolTest {

    @Test
    void subProtocol_values() {
        assertThat(OcppProtocol.OCPP_16.subProtocol()).isEqualTo("ocpp1.6");
        assertThat(OcppProtocol.OCPP_201.subProtocol()).isEqualTo("ocpp2.0.1");
    }

    @Test
    void fromSubProtocol_valid() {
        assertThat(OcppProtocol.fromSubProtocol("ocpp1.6")).isEqualTo(OcppProtocol.OCPP_16);
        assertThat(OcppProtocol.fromSubProtocol("ocpp2.0.1")).isEqualTo(OcppProtocol.OCPP_201);
    }

    @Test
    void fromSubProtocol_unknown_returnsNull() {
        assertThat(OcppProtocol.fromSubProtocol("ocpp3.0")).isNull();
        assertThat(OcppProtocol.fromSubProtocol("")).isNull();
        // Non-spec variants observed in the wild (e.g. eNovates ENOGEN firmware sends
        // "ocpp2.01"). Per OCPP-J §3.1.2 only the IANA-registered names are valid; reject.
        assertThat(OcppProtocol.fromSubProtocol("ocpp2.01")).isNull();
    }
}
