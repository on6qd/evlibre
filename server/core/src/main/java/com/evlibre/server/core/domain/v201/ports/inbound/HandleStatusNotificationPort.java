package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.v201.dto.StatusNotificationData201;

public interface HandleStatusNotificationPort {

    void statusNotification(StatusNotificationData201 data);
}
