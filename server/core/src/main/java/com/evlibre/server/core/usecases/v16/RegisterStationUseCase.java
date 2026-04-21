package com.evlibre.server.core.usecases.v16;

import com.evlibre.server.core.domain.shared.dto.RegistrationResult;
import com.evlibre.server.core.domain.shared.dto.StationRegistration;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.v16.ports.inbound.RegisterStationPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationEventPublisher;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RegisterStationUseCase implements RegisterStationPort {

    private static final Logger log = LoggerFactory.getLogger(RegisterStationUseCase.class);

    private final TenantRepositoryPort tenantRepository;
    private final StationRepositoryPort stationRepository;
    private final OcppEventLogPort eventLog;
    private final TimeProvider timeProvider;
    private final int heartbeatInterval;
    private final StationEventPublisher stationEventPublisher;

    public RegisterStationUseCase(TenantRepositoryPort tenantRepository,
                                  StationRepositoryPort stationRepository,
                                  OcppEventLogPort eventLog,
                                  TimeProvider timeProvider,
                                  int heartbeatInterval,
                                  StationEventPublisher stationEventPublisher) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository);
        this.stationRepository = Objects.requireNonNull(stationRepository);
        this.eventLog = Objects.requireNonNull(eventLog);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        this.heartbeatInterval = heartbeatInterval;
        this.stationEventPublisher = Objects.requireNonNull(stationEventPublisher);
    }

    @Override
    public RegistrationResult register(StationRegistration registration) {
        Instant now = timeProvider.now();

        eventLog.logEvent(
                registration.identity().value(),
                null,
                "BootNotification",
                "IN",
                String.format("vendor=%s model=%s", registration.vendor(), registration.model())
        );

        var tenant = tenantRepository.findByTenantId(registration.tenantId());
        if (tenant.isEmpty()) {
            log.warn("BootNotification from unknown tenant: {}", registration.tenantId().value());
            return new RegistrationResult(RegistrationStatus.REJECTED, now, heartbeatInterval);
        }

        var existing = stationRepository.findByTenantAndIdentity(
                registration.tenantId(), registration.identity());

        ChargingStation station;
        if (existing.isPresent()) {
            station = ChargingStation.builder()
                    .from(existing.get())
                    .vendor(registration.vendor())
                    .model(registration.model())
                    .serialNumber(registration.serialNumber())
                    .firmwareVersion(registration.firmwareVersion())
                    .protocol(registration.protocol())
                    .registrationStatus(RegistrationStatus.ACCEPTED)
                    .lastBootNotification(now)
                    .build();
            log.info("Re-registration of station {} for tenant {}",
                    registration.identity().value(), registration.tenantId().value());
        } else {
            station = ChargingStation.builder()
                    .id(UUID.randomUUID())
                    .tenantId(registration.tenantId())
                    .identity(registration.identity())
                    .protocol(registration.protocol())
                    .vendor(registration.vendor())
                    .model(registration.model())
                    .serialNumber(registration.serialNumber())
                    .firmwareVersion(registration.firmwareVersion())
                    .registrationStatus(RegistrationStatus.ACCEPTED)
                    .lastBootNotification(now)
                    .createdAt(now)
                    .build();
            log.info("New station registered: {} for tenant {}",
                    registration.identity().value(), registration.tenantId().value());
        }

        stationRepository.save(station);
        stationEventPublisher.stationUpdated(registration.tenantId(), registration.identity());

        return new RegistrationResult(RegistrationStatus.ACCEPTED, now, heartbeatInterval);
    }
}
