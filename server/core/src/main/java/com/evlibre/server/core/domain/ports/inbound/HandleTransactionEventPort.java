package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.TransactionEventData;
import com.evlibre.server.core.domain.dto.TransactionEventResult;

public interface HandleTransactionEventPort {

    TransactionEventResult transactionEvent(TransactionEventData data);
}
