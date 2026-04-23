package com.evlibre.server.core.domain.v201.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdTokenInfoTest {

    @Test
    void accepted_factory_produces_minimal_info() {
        IdTokenInfo info = IdTokenInfo.accepted();

        assertThat(info.status()).isEqualTo(AuthorizationStatus.ACCEPTED);
        assertThat(info.cacheExpiryDateTime()).isNull();
        assertThat(info.chargingPriority()).isNull();
        assertThat(info.evseId()).isNull();
        assertThat(info.groupIdToken()).isNull();
    }

    @Test
    void charging_priority_lower_bound_accepted() {
        IdTokenInfo info = new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, -9, null, null, null, null, null);

        assertThat(info.chargingPriority()).isEqualTo(-9);
    }

    @Test
    void charging_priority_upper_bound_accepted() {
        IdTokenInfo info = new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, 9, null, null, null, null, null);

        assertThat(info.chargingPriority()).isEqualTo(9);
    }

    @Test
    void charging_priority_below_range_rejected() {
        assertThatThrownBy(() -> new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, -10, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chargingPriority");
    }

    @Test
    void charging_priority_above_range_rejected() {
        assertThatThrownBy(() -> new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, 10, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chargingPriority");
    }

    @Test
    void empty_evse_id_list_rejected() {
        assertThatThrownBy(() -> new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, null, List.of(), null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evseId");
    }

    @Test
    void evse_id_list_defensively_copied() {
        var mutable = new java.util.ArrayList<Integer>();
        mutable.add(1);
        IdTokenInfo info = new IdTokenInfo(
                AuthorizationStatus.ACCEPTED, null, null, mutable, null, null, null, null);

        mutable.add(2);

        assertThat(info.evseId()).containsExactly(1);
    }

    @Test
    void null_status_rejected() {
        assertThatThrownBy(() -> new IdTokenInfo(
                null, null, null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
