package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.handler.OcppMessageHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class OcppMessageDispatcher {

    private final Map<String, OcppMessageHandler> handlers = new ConcurrentHashMap<>();

    public void registerHandler(OcppProtocol protocol, String action, OcppMessageHandler handler) {
        handlers.put(handlerKey(protocol, action), handler);
    }

    public Optional<OcppMessageHandler> getHandler(OcppProtocol protocol, String action) {
        return Optional.ofNullable(handlers.get(handlerKey(protocol, action)));
    }

    private String handlerKey(OcppProtocol protocol, String action) {
        return protocol.name() + ":" + action;
    }
}
