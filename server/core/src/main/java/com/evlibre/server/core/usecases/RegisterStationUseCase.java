package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.RegistrationResult;
import com.evlibre.server.core.domain.dto.StationRegistration;
import com.evlibre.server.core.domain.model.ChargingStation;
import com.evlibre.server.core.domain.model.RegistrationStatus;
import com.evlibre.server.core.domain.ports.inbound.RegisterStationPort;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TenantRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TimeProvider;
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

    public RegisterStationUseCase(TenantRepositoryPort tenantRepository,
                                  StationRepositoryPort stationRepository,
                                  OcppEventLogPort eventLog,
                                  TimeProvider timeProvider,
                                  int heartbeatInterval) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository);
        this.stationRepository = Objects.requireNonNull(stationRepository);
        this.eventLog = Objects.requireNonNull(eventLog);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        this.heartbeatInterval = heartbeatInterval;
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

        return new RegistrationResult(RegistrationStatus.ACCEPTED, now, heartbeatInterval);
    }
}
