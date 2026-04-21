package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.StartTransactionData;
import com.evlibre.server.core.domain.v16.dto.StartTransactionResult;

public interface StartTransactionPort {

    StartTransactionResult startTransaction(StartTransactionData data);
}
