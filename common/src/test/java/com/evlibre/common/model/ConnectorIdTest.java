package com.evlibre.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorIdTest {

    @Test
    void valid_connector() {
        assertThat(new ConnectorId(0).value()).isEqualTo(0);
        assertThat(new ConnectorId(1).value()).isEqualTo(1);
    }

    @Test
    void negative_throws() {
        assertThatThrownBy(() -> new ConnectorId(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mainController() {
        assertThat(new ConnectorId(0).isMainController()).isTrue();
        assertThat(new ConnectorId(1).isMainController()).isFalse();
    }

    @Test
    void equality() {
        assertThat(new ConnectorId(1)).isEqualTo(new ConnectorId(1));
        assertThat(new ConnectorId(1)).isNotEqualTo(new ConnectorId(2));
    }
}
