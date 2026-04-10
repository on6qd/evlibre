package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.StopTransactionData;
import com.evlibre.server.core.domain.model.Transaction;
import com.evlibre.server.core.domain.model.TransactionStatus;
import com.evlibre.server.core.domain.ports.inbound.StopTransactionPort;
import com.evlibre.server.core.domain.ports.outbound.TransactionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class StopTransactionUseCase implements StopTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(StopTransactionUseCase.class);

    private final TransactionRepositoryPort transactionRepository;

    public StopTransactionUseCase(TransactionRepositoryPort transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    @Override
    public void stopTransaction(StopTransactionData data) {
        var existing = transactionRepository.findByOcppTransactionId(data.tenantId(), data.transactionId());

        if (existing.isEmpty()) {
            log.warn("StopTransaction for unknown transactionId={} tenant={}",
                    data.transactionId(), data.tenantId().value());
            return;
        }

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
}
