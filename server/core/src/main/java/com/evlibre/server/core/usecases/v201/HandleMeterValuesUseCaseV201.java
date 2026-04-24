package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.shared.ports.outbound.OcppEventLogPort;
import com.evlibre.server.core.domain.v201.dto.MeterValuesData201;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleMeterValuesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleMeterValuesUseCaseV201 implements HandleMeterValuesPort {

    private static final Logger log = LoggerFactory.getLogger(HandleMeterValuesUseCaseV201.class);

    private final OcppEventLogPort eventLog;

    public HandleMeterValuesUseCaseV201(OcppEventLogPort eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public void meterValues(MeterValuesData201 data) {
        log.info("MeterValues from {} evse {}: {} readings",
                data.stationIdentity().value(), data.evseId().value(),
                data.meterValues().size());

        eventLog.logEvent(
                data.stationIdentity().value(),
                null,
                "MeterValues",
                "IN",
                String.format("evse=%d readings=%d",
                        data.evseId().value(), data.meterValues().size())
        );
    }
}
