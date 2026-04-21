package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.MeterValuesData;

public interface HandleMeterValuesPort {

    void meterValues(MeterValuesData data);
}
