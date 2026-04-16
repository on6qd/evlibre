package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.StartTransactionData;
import com.evlibre.server.core.domain.dto.StartTransactionResult;
import com.evlibre.server.core.domain.model.ReservationStatus;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.model.TransactionStatus;
import com.evlibre.server.core.domain.ports.inbound.AuthorizePort;
import com.evlibre.server.core.domain.ports.inbound.StartTransactionPort;
import com.evlibre.server.core.domain.ports.outbound.ReservationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.StationRepositoryPort;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;
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
        if (data.reservationId() != null) {
            reservationRepository.findByReservationId(data.tenantId(), data.reservationId())
                    .filter(r -> r.status() == ReservationStatus.ACTIVE)
                    .ifPresent(r -> reservationRepository.save(r.withStatus(ReservationStatus.USED)));
        }

        log.info("StartTransaction id={} ocppTxId={} station={} connector={} idTag={} reservationId={}",
                transaction.id(), ocppTransactionId,
                data.stationIdentity().value(), data.connectorId().value(), data.idTag(), data.reservationId());

        return new StartTransactionResult(ocppTransactionId, authResult.status(),
                authResult.expiryDate(), authResult.parentIdTag());
    }
}
