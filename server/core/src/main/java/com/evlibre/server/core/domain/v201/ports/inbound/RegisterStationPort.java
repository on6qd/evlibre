package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.shared.dto.RegistrationResult;
import com.evlibre.server.core.domain.shared.dto.StationRegistration;

public interface RegisterStationPort {

    RegistrationResult register(StationRegistration registration);
}
