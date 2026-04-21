package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.v16.dto.StatusNotificationData;

public interface HandleStatusNotificationPort {

    void statusNotification(StatusNotificationData data);
}
