package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OcppSessionManager {

    private static final Logger log = LoggerFactory.getLogger(OcppSessionManager.class);

    private final Map<String, OcppSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, PingState> pingStates = new ConcurrentHashMap<>();

    public void register(OcppSession session) {
        String key = sessionKey(session.tenantId(), session.stationIdentity());
        sessions.put(key, session);
        log.info("Station connected: {} (tenant: {}, protocol: {})",
                session.stationIdentity().value(), session.tenantId().value(), session.protocol());
    }

    public void unregister(TenantId tenantId, ChargePointIdentity stationIdentity) {
        String key = sessionKey(tenantId, stationIdentity);
        sessions.remove(key);
        pingStates.remove(key);
        log.info("Station disconnected: {} (tenant: {})",
                stationIdentity.value(), tenantId.value());
    }

    public Optional<OcppSession> getSession(TenantId tenantId, ChargePointIdentity stationIdentity) {
        String key = sessionKey(tenantId, stationIdentity);
        return Optional.ofNullable(sessions.get(key));
    }

    public boolean isConnected(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return sessions.containsKey(sessionKey(tenantId, stationIdentity));
    }

    public Set<ChargePointIdentity> connectedStations(TenantId tenantId) {
        return sessions.values().stream()
                .filter(s -> s.tenantId().equals(tenantId))
                .map(OcppSession::stationIdentity)
                .collect(Collectors.toSet());
    }

    // --- Ping/pong timer tracking ---

    public void setPingTimerId(TenantId tenantId, ChargePointIdentity stationIdentity, long timerId) {
        String key = sessionKey(tenantId, stationIdentity);
        pingStates.compute(key, (k, state) ->
                new PingState(timerId, state != null ? state.deadlineTimerId : -1));
    }

    public void setDeadlineTimerId(TenantId tenantId, ChargePointIdentity stationIdentity, long timerId) {
        String key = sessionKey(tenantId, stationIdentity);
        pingStates.compute(key, (k, state) ->
                new PingState(state != null ? state.pingTimerId : -1, timerId));
    }

    public PingState getPingState(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return pingStates.getOrDefault(sessionKey(tenantId, stationIdentity), PingState.NONE);
    }

    private String sessionKey(TenantId tenantId, ChargePointIdentity stationIdentity) {
        return tenantId.value() + ":" + stationIdentity.value();
    }

    public record PingState(long pingTimerId, long deadlineTimerId) {
        static final PingState NONE = new PingState(-1, -1);
    }
}
