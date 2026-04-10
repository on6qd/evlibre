package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.MeterValuesData;

public interface HandleMeterValuesPort {

    void meterValues(MeterValuesData data);
}
