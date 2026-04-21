package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.v201.dto.TransactionEventData;
import com.evlibre.server.core.domain.v201.dto.TransactionEventResult;

public interface HandleTransactionEventPort {

    TransactionEventResult transactionEvent(TransactionEventData data);
}
