package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.AuthorizationResult;
import com.evlibre.server.core.domain.v16.dto.StopTransactionData;

import java.util.Optional;

public interface StopTransactionPort {

    /**
     * OCPP 1.6 §5.28: if the request carries an idTag, the CSMS authorizes it and
     * returns the resulting idTagInfo in StopTransaction.conf. The response is
     * otherwise empty.
     */
    Optional<AuthorizationResult> stopTransaction(StopTransactionData data);
}
