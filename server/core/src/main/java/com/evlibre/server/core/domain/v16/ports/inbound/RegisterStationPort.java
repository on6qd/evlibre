package com.evlibre.server.core.domain.v16.ports.inbound;

import com.evlibre.server.core.domain.shared.dto.RegistrationResult;
import com.evlibre.server.core.domain.shared.dto.StationRegistration;

public interface RegisterStationPort {

    RegistrationResult register(StationRegistration registration);
}
