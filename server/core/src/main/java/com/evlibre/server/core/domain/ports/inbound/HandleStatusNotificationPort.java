package com.evlibre.server.core.domain.ports.inbound;

import com.evlibre.server.core.domain.dto.StatusNotificationData;

public interface HandleStatusNotificationPort {

    void statusNotification(StatusNotificationData data);
}
