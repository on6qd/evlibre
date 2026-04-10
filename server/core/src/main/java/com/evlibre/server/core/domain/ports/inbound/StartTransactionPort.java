package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.StartTransactionData;
import com.evlibre.server.core.domain.dto.StartTransactionResult;

public interface StartTransactionPort {

    StartTransactionResult startTransaction(StartTransactionData data);
}
