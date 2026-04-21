package com.evlibre.server.core.usecases.v16;

import com.evlibre.server.core.domain.v16.dto.AuthorizationResult;
import com.evlibre.server.core.domain.v16.dto.StopTransactionData;
import com.evlibre.server.core.domain.v16.model.Transaction;
import com.evlibre.server.core.domain.shared.model.TransactionStatus;
import com.evlibre.server.core.domain.v16.ports.inbound.AuthorizePort;
import com.evlibre.server.core.domain.v16.ports.inbound.StopTransactionPort;
import com.evlibre.server.core.domain.v16.ports.outbound.TransactionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class StopTransactionUseCase implements StopTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(StopTransactionUseCase.class);

    private final TransactionRepositoryPort transactionRepository;
    private final AuthorizePort authorizePort;

    public StopTransactionUseCase(TransactionRepositoryPort transactionRepository) {
        this(transactionRepository, null);
    }

    public StopTransactionUseCase(TransactionRepositoryPort transactionRepository, AuthorizePort authorizePort) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
        this.authorizePort = authorizePort;
    }

    @Override
    public Optional<AuthorizationResult> stopTransaction(StopTransactionData data) {
        // OCPP 1.6 §5.28 / §5.27 errata: respond even for unknown/-1 transactionId to
        // unblock the CP's message queue. The update below is best-effort.
        var existing = transactionRepository.findByOcppTransactionId(data.tenantId(), data.transactionId());

        if (existing.isEmpty()) {
            log.warn("StopTransaction for unknown transactionId={} tenant={}",
                    data.transactionId(), data.tenantId().value());
        } else {
            var transaction = existing.get();
            var updated = Transaction.builder()
                    .from(transaction)
                    .stopTime(data.timestamp())
                    .meterStop(data.meterStop())
                    .status(TransactionStatus.STOPPED_COMPLETED)
                    .stopReason(data.reason())
                    .build();
            transactionRepository.save(updated);
            log.info("StopTransaction ocppTxId={} station={} meterStop={} reason={}",
                    data.transactionId(), data.stationIdentity().value(),
                    data.meterStop(), data.reason());
        }

        // §5.28: authorize the stopping idTag if provided. The CP uses this to decide
        // whether to accept the stop request, but the CSMS cannot prevent a transaction
        // from stopping once StopTransaction.req is sent.
        if (data.idTag() == null || data.idTag().isBlank() || authorizePort == null) {
            return Optional.empty();
        }
        return Optional.of(authorizePort.authorize(data.tenantId(), data.idTag()));
    }
}
