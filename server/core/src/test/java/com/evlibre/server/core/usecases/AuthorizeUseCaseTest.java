package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.AuthorizationResult;
import com.evlibre.server.core.domain.model.*;
import com.evlibre.server.core.domain.ports.outbound.AuthorizationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizeUseCaseTest {

    private FakeAuthorizationRepository authRepo;
    private AuthorizeUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");

    @BeforeEach
    void setUp() {
        authRepo = new FakeAuthorizationRepository();
        useCase = new AuthorizeUseCase(authRepo);
    }

    @Test
    void known_accepted_tag_returns_accepted() {
        authRepo.addAuthorization(tenantId, "TAG001", AuthorizationStatus.ACCEPTED);

        AuthorizationResult result = useCase.authorize(tenantId, "TAG001");

        assertThat(result.status()).isEqualTo(AuthorizationStatus.ACCEPTED);
        assertThat(result.idTag()).isEqualTo("TAG001");
    }

    @Test
    void unknown_tag_defaults_to_invalid() {
        AuthorizationResult result = useCase.authorize(tenantId, "UNKNOWN-TAG");

        assertThat(result.status()).isEqualTo(AuthorizationStatus.INVALID);
        assertThat(result.idTag()).isEqualTo("UNKNOWN-TAG");
    }

    @Test
    void blocked_tag_returns_blocked() {
        authRepo.addAuthorization(tenantId, "BLOCKED-TAG", AuthorizationStatus.BLOCKED);

        AuthorizationResult result = useCase.authorize(tenantId, "BLOCKED-TAG");

        assertThat(result.status()).isEqualTo(AuthorizationStatus.BLOCKED);
        assertThat(result.idTag()).isEqualTo("BLOCKED-TAG");
    }

    @Test
    void authorize_returns_expiry_and_parent() {
        var expiry = java.time.Instant.parse("2030-01-01T00:00:00Z");
        authRepo.addAuthorization(tenantId, "TAG-A",
                new com.evlibre.server.core.domain.model.IdTagInfo(
                        AuthorizationStatus.ACCEPTED, expiry, "PARENT-X"));

        AuthorizationResult result = useCase.authorize(tenantId, "TAG-A");

        assertThat(result.status()).isEqualTo(AuthorizationStatus.ACCEPTED);
        assertThat(result.expiryDate()).isEqualTo(expiry);
        assertThat(result.parentIdTag()).isEqualTo("PARENT-X");
    }

    @Test
    void authorize_expired_tag_returns_expired_status() {
        var past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        var fixedClock = new com.evlibre.server.core.domain.ports.outbound.TimeProvider() {
            @Override public java.time.Instant now() { return java.time.Instant.parse("2025-01-15T10:00:00Z"); }
        };
        var useCaseWithClock = new AuthorizeUseCase(authRepo, fixedClock);
        authRepo.addAuthorization(tenantId, "TAG-B",
                new com.evlibre.server.core.domain.model.IdTagInfo(
                        AuthorizationStatus.ACCEPTED, past, null));

        AuthorizationResult result = useCaseWithClock.authorize(tenantId, "TAG-B");

        assertThat(result.status()).isEqualTo(AuthorizationStatus.EXPIRED);
    }

    // --- Fakes ---

    static class FakeAuthorizationRepository implements AuthorizationRepositoryPort {
        private final Map<String, com.evlibre.server.core.domain.model.IdTagInfo> store = new ConcurrentHashMap<>();

        void addAuthorization(TenantId tenantId, String idTag, AuthorizationStatus status) {
            store.put(tenantId.value() + ":" + idTag,
                    com.evlibre.server.core.domain.model.IdTagInfo.of(status));
        }

        void addAuthorization(TenantId tenantId, String idTag,
                               com.evlibre.server.core.domain.model.IdTagInfo info) {
            store.put(tenantId.value() + ":" + idTag, info);
        }

        @Override
        public Optional<com.evlibre.server.core.domain.model.IdTagInfo> findInfoByIdTag(
                TenantId tenantId, String idTag) {
            return Optional.ofNullable(store.get(tenantId.value() + ":" + idTag));
        }
    }
}
