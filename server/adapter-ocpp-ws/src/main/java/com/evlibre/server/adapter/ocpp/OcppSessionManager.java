package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class OcppSessionManager {

    private static final Logger log = LoggerFactory.getLogger(OcppSessionManager.class);

    private final Map<String, OcppSession> sessions = new ConcurrentHashMap<>();

    public void register(OcppSession session) {
        String key = sessionKey(session.tenantId(), session.stationIdentity());
        sessions.put(key, session);
        log.info("Station connected: {} (tenant: {}, protocol: {})",
                session.stationIdentity().value(), session.tenantId().value(), session.protocol());
    }

    public void unregister(TenantId tenantId, ChargePointIdentity stationIdentity) {
        String key = sessionKey(tenantId, stationIdentity);
        sessions.remove(key);
        log.info("Station disconnected: {} (tenant: {})",
                stationIdentity.value(), tenantId.value());
    }

    public Optional<OcppSession> getSession(TenantId tenantId, ChargePointIdentity stationIdentity) {
        String key = sessionKey(tenantId, stationIdentity);
        return Optional.ofNullable(sessions.get(key));
    }

    private String sessionKey(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return tenantId.value() + ":" + stationIdentity.value();
    }
}
