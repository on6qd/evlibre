package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.StatusNotificationData;

public interface HandleStatusNotificationPort {

    void statusNotification(StatusNotificationData data);
}
