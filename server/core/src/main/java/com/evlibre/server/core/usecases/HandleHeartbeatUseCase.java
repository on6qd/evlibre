package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.ports.inbound.HandleHeartbeatPort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationEventPublisher;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class HandleHeartbeatUseCase implements HandleHeartbeatPort {

    private static final Logger log = LoggerFactory.getLogger(HandleHeartbeatUseCase.class);

    private final StationRepositoryPort stationRepository;
    private final TimeProvider timeProvider;
    private final StationEventPublisher stationEventPublisher;

    public HandleHeartbeatUseCase(StationRepositoryPort stationRepository,
                                  TimeProvider timeProvider,
                                  StationEventPublisher stationEventPublisher) {
        this.stationRepository = stationRepository;
        this.timeProvider = timeProvider;
        this.stationEventPublisher = stationEventPublisher;
    }

    @Override
    public Instant heartbeat(TenantId tenantId, ChargePointIdentity stationIdentity) {
        Instant now = timeProvider.now();

        stationRepository.findByTenantAndIdentity(tenantId, stationIdentity)
                .ifPresent(station -> {
                    stationRepository.save(station.receiveHeartbeat(now));
                    stationEventPublisher.stationUpdated(tenantId, stationIdentity);
                    log.debug("Heartbeat from {}", stationIdentity.value());
                });

        return now;
    }
}
