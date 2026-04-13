package com.evlibre.server.adapter.webui.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.ChargingStation;

import java.util.Set;

public record StationView(
        String stationId,
        String vendor,
        String model,
        String serialNumber,
        String firmwareVersion,
        String protocol,
        String registrationStatus,
        boolean online,
        String statusBadgeClass,
        String registrationBadgeClass
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
                online,
                online ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800",
                getRegistrationBadgeClass(station.registrationStatus().name())
        );
    }

    private static String getRegistrationBadgeClass(String status) {
        return switch (status) {
            case "ACCEPTED" -> "bg-green-100 text-green-800";
            case "PENDING" -> "bg-yellow-100 text-yellow-800";
            case "REJECTED" -> "bg-red-100 text-red-800";
            default -> "bg-gray-100 text-gray-800";
        };
    }
}
