package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.MeterValuesData;

public interface HandleMeterValuesPort {

    void meterValues(MeterValuesData data);
}
