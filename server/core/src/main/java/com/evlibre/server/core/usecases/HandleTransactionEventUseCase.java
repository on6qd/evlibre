package com.evlibre.server.core.usecases;

import com.evlibre.server.core.domain.dto.TransactionEventData;
import com.evlibre.server.core.domain.dto.TransactionEventResult;
import com.evlibre.server.core.domain.ports.inbound.HandleTransactionEventPort;
import com.evlibre.server.core.domain.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleTransactionEventUseCase implements HandleTransactionEventPort {

    private static final Logger log = LoggerFactory.getLogger(HandleTransactionEventUseCase.class);

    private final OcppEventLogPort eventLog;

    public HandleTransactionEventUseCase(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public TransactionEventResult transactionEvent(TransactionEventData data) {
        log.info("TransactionEvent [{}] from {} tx={} trigger={} evse={} connector={}",
                data.eventType(), data.stationIdentity().value(),
                data.transactionId(), data.triggerReason(),
                data.evseId() != null ? data.evseId().value() : "n/a",
                data.connectorId() != null ? data.connectorId().value() : "n/a");

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "TransactionEvent",
                "IN",
                String.format("eventType=%s tx=%s trigger=%s idToken=%s",
                        data.eventType(), data.transactionId(),
                        data.triggerReason(), data.idToken())
        );

        return new TransactionEventResult(0);
    }
}
