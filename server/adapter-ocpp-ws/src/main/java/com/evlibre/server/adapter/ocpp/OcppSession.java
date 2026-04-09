package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.model.TenantId;
import io.vertx.core.http.ServerWebSocket;

public record OcppSession(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        OcppProtocol protocol,
        ServerWebSocket webSocket
) {}
