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

    // --- Fakes ---

    static class FakeAuthorizationRepository implements AuthorizationRepositoryPort {
        private final Map<String, AuthorizationStatus> store = new ConcurrentHashMap<>();

        void addAuthorization(TenantId tenantId, String idTag, AuthorizationStatus status) {
            store.put(tenantId.value() + ":" + idTag, status);
        }

        @Override
        public Optional<AuthorizationStatus> findStatusByIdTag(TenantId tenantId, String idTag) {
            return Optional.ofNullable(store.get(tenantId.value() + ":" + idTag));
        }
    }
}
