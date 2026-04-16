package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.AuthorizationResult;
import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.evlibre.server.core.domain.ports.outbound.AuthorizationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AuthorizeUseCase implements AuthorizePort {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeUseCase.class);

    private final AuthorizationRepositoryPort authorizationRepository;

    public AuthorizeUseCase(AuthorizationRepositoryPort authorizationRepository) {
        this.authorizationRepository = Objects.requireNonNull(authorizationRepository);
    }

    @Override
    public AuthorizationResult authorize(TenantId tenantId, String idTag) {
        var status = authorizationRepository.findStatusByIdTag(tenantId, idTag)
                .orElse(AuthorizationStatus.INVALID);

        log.info("Authorize idTag={} tenant={} -> {}", idTag, tenantId.value(), status);

        return new AuthorizationResult(status, idTag);
    }
}
