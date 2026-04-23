package com.evlibre.server.core.domain.v201.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdTokenTest {

    @Test
    void of_creates_token_without_additional_info() {
        IdToken token = IdToken.of("ABC-123", IdTokenType.ISO14443);

        assertThat(token.idToken()).isEqualTo("ABC-123");
        assertThat(token.type()).isEqualTo(IdTokenType.ISO14443);
        assertThat(token.additionalInfo()).isNull();
    }

    @Test
    void additional_info_list_is_defensively_copied() {
        var mutable = new java.util.ArrayList<AdditionalInfo>();
        mutable.add(new AdditionalInfo("alt-1", "parent"));
        IdToken token = new IdToken("driver-42", IdTokenType.CENTRAL, mutable);

        mutable.add(new AdditionalInfo("alt-2", "parent"));

        assertThat(token.additionalInfo()).hasSize(1);
        assertThat(token.additionalInfo().get(0).additionalIdToken()).isEqualTo("alt-1");
    }

    @Test
    void empty_additional_info_rejected() {
        assertThatThrownBy(() -> new IdToken("x", IdTokenType.CENTRAL, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("additionalInfo");
    }

    @Test
    void null_idToken_rejected() {
        assertThatThrownBy(() -> IdToken.of(null, IdTokenType.CENTRAL))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idToken");
    }

    @Test
    void null_type_rejected() {
        assertThatThrownBy(() -> IdToken.of("x", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    void additional_info_requires_both_fields() {
        assertThatThrownBy(() -> new AdditionalInfo(null, "parent"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("additionalIdToken");
        assertThatThrownBy(() -> new AdditionalInfo("x", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }
}
