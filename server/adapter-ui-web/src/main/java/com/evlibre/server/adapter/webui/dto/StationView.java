package com.evlibre.server.adapter.webui.dto;

import com.evlibre.server.core.domain.model.ChargingStation;

import java.time.Duration;
import java.time.Instant;

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

    public static StationView fromDomain(ChargingStation station) {
        boolean online = isOnline(station);

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

    private static boolean isOnline(ChargingStation station) {
        if (station.lastHeartbeat() == null) return false;
        return Duration.between(station.lastHeartbeat(), Instant.now()).getSeconds() < 900;
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
