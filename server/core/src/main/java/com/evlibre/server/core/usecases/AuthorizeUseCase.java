package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.AuthorizationResult;
import com.evlibre.server.core.domain.model.AuthorizationStatus;
import com.evlibre.server.core.domain.model.IdTagInfo;
import com.evlibre.server.core.domain.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.evlibre.server.core.domain.ports.outbound.AuthorizationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

public class AuthorizeUseCase implements AuthorizePort {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeUseCase.class);

    private final AuthorizationRepositoryPort authorizationRepository;
    private final TimeProvider timeProvider;

    public AuthorizeUseCase(AuthorizationRepositoryPort authorizationRepository) {
        this(authorizationRepository, Instant::now);
    }

    public AuthorizeUseCase(AuthorizationRepositoryPort authorizationRepository, TimeProvider timeProvider) {
        this.authorizationRepository = Objects.requireNonNull(authorizationRepository);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public AuthorizationResult authorize(TenantId tenantId, String idTag) {
        IdTagInfo info = authorizationRepository.findInfoByIdTag(tenantId, idTag)
                .map(this::applyExpiry)
                .orElse(IdTagInfo.of(AuthorizationStatus.INVALID));

        log.info("Authorize idTag={} tenant={} -> {}", idTag, tenantId.value(), info.status());

        return AuthorizationResult.from(info, idTag);
    }

    // OCPP 1.6 §7.8: if expiryDate is in the past, the token is Expired.
    private IdTagInfo applyExpiry(IdTagInfo info) {
        if (info.expiryDate() != null && info.expiryDate().isBefore(timeProvider.now())) {
            return new IdTagInfo(AuthorizationStatus.EXPIRED, info.expiryDate(), info.parentIdTag());
        }
        return info;
    }
}
