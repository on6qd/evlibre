package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.StartTransactionData;
import com.evlibre.server.core.domain.v16.dto.StartTransactionResult;

public interface StartTransactionPort {

    StartTransactionResult startTransaction(StartTransactionData data);
}
