package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.StopTransactionData;

public interface StopTransactionPort {

    void stopTransaction(StopTransactionData data);
}
