package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.v16.dto.MeterValuesData;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleMeterValuesPort;
import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleMeterValuesUseCaseV201 implements HandleMeterValuesPort {

    private static final Logger log = LoggerFactory.getLogger(HandleMeterValuesUseCaseV201.class);

    private final OcppEventLogPort eventLog;

    public HandleMeterValuesUseCaseV201(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void meterValues(MeterValuesData data) {
        log.info("MeterValues from {} connector {} (tx: {}): {} readings",
                data.stationIdentity().value(), data.connectorId().value(),
                data.transactionId(), data.meterValues().size());

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "MeterValues",
                "IN",
                String.format("connector=%d tx=%s readings=%d",
                        data.connectorId().value(), data.transactionId(), data.meterValues().size())
        );
    }
}
