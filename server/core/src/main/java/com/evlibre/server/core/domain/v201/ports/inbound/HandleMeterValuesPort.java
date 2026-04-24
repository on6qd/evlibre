package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.v201.dto.MeterValuesData201;

public interface HandleMeterValuesPort {

    void meterValues(MeterValuesData201 data);
}
