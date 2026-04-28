package com.evlibre.server.adapter.webui.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.ChargingStation;

import java.util.Set;

public record StationView(
        String stationId,
        String vendor,
        String model,
        String serialNumber,
        String firmwareVersion,
        String protocol,
        String registrationStatus,
        boolean online
) {

    public static StationView fromDomain(ChargingStation station, Set<ChargePointIdentity> connectedStations) {
        boolean online = connectedStations.contains(station.identity());

        return new StationView(
                station.identity().value(),
                station.vendor(),
                station.model(),
                station.serialNumber() != null ? station.serialNumber() : "-",
                station.firmwareVersion() != null ? station.firmwareVersion() : "-",
                station.protocol().name(),
                station.registrationStatus().name(),
                online
        );
    }
}
