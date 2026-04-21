package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.v16.dto.StartTransactionData;
import com.evlibre.server.core.domain.v16.dto.StartTransactionResult;
import com.evlibre.server.core.domain.v16.model.ReservationStatus;
import com.evlibre.server.core.domain.v16.model.Transaction;
import com.evlibre.server.core.domain.shared.model.TransactionStatus;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.evlibre.server.core.domain.ports.inbound.StartTransactionPort;
import com.evlibre.server.core.domain.v16.ports.outbound.ReservationRepositoryPort;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public class StartTransactionUseCase implements StartTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(StartTransactionUseCase.class);

    private final AuthorizePort authorizePort;
    private final TransactionRepositoryPort transactionRepository;
    private final StationRepositoryPort stationRepository;
    private final ReservationRepositoryPort reservationRepository;

    public StartTransactionUseCase(AuthorizePort authorizePort,
                                   TransactionRepositoryPort transactionRepository,
                                   StationRepositoryPort stationRepository,
                                   ReservationRepositoryPort reservationRepository) {
        this.authorizePort = Objects.requireNonNull(authorizePort);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.stationRepository = Objects.requireNonNull(stationRepository);
        this.reservationRepository = Objects.requireNonNull(reservationRepository);
    }

    @Override
    public StartTransactionResult startTransaction(StartTransactionData data) {
        var authResult = authorizePort.authorize(data.tenantId(), data.idTag());

        int ocppTransactionId = transactionRepository.nextOcppTransactionId();

        var station = stationRepository.findByTenantAndIdentity(data.tenantId(), data.stationIdentity());
        UUID stationId = station.map(s -> s.id()).orElse(UUID.randomUUID());

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .tenantId(data.tenantId())
                .ocppTransactionId(ocppTransactionId)
                .stationId(stationId)
                .stationIdentity(data.stationIdentity())
                .connectorId(data.connectorId())
                .idTag(data.idTag())
                .startTime(data.timestamp())
                .meterStart(data.meterStart())
                .status(TransactionStatus.IN_PROGRESS)
                .createdAt(data.timestamp())
                .build();

        transactionRepository.save(transaction);

        // OCPP 1.6 §6: presence of reservationId in StartTransaction.req terminates the reservation.
        // Expired reservations cannot transition to USED; just mark them EXPIRED and move on.
        // ConnectorId matching: reservation.connectorId=0 means station-wide (any connector OK),
        // reservation.connectorId>0 requires an exact match with the StartTransaction connector.
        if (data.reservationId() != null) {
            reservationRepository.findByReservationId(data.tenantId(), data.reservationId())
                    .ifPresent(r -> {
                        ReservationStatus effective = r.effectiveStatus(data.timestamp());
                        boolean connectorMatches = r.connectorId() == 0
                                || r.connectorId() == data.connectorId().value();
                        if (effective == ReservationStatus.ACTIVE && connectorMatches) {
                            reservationRepository.save(r.withStatus(ReservationStatus.USED));
                        } else if (effective == ReservationStatus.EXPIRED && r.status() != ReservationStatus.EXPIRED) {
                            reservationRepository.save(r.withStatus(ReservationStatus.EXPIRED));
                        } else if (effective == ReservationStatus.ACTIVE && !connectorMatches) {
                            log.warn("StartTransaction reservationId={} bound to connector {} but tx on connector {} — not consuming reservation",
                                    data.reservationId(), r.connectorId(), data.connectorId().value());
                        }
                    });
        }

        log.info("StartTransaction id={} ocppTxId={} station={} connector={} idTag={} reservationId={}",
                transaction.id(), ocppTransactionId,
                data.stationIdentity().value(), data.connectorId().value(), data.idTag(), data.reservationId());

        return new StartTransactionResult(ocppTransactionId, authResult.status(),
                authResult.expiryDate(), authResult.parentIdTag());
    }
}
