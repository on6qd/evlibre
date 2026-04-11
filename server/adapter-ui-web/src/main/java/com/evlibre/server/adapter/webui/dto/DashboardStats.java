package com.evlibre.server.adapter.webui.dto;

import com.evlibre.server.core.domain.model.ChargingStation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record DashboardStats(
        int totalStations,
        int onlineStations,
        int offlineStations,
        double uptimePercentage
) {

    public static DashboardStats calculate(List<ChargingStation> stations) {
        int online = (int) stations.stream()
                .filter(DashboardStats::isOnline)
                .count();
        int offline = stations.size() - online;

        double uptime = stations.isEmpty()
                ? 100.0
                : (online * 100.0) / stations.size();

        return new DashboardStats(
                stations.size(),
                online,
                offline,
                Math.round(uptime * 10.0) / 10.0
        );
    }

    private static boolean isOnline(ChargingStation station) {
        if (station.lastHeartbeat() == null) return false;
        return Duration.between(station.lastHeartbeat(), Instant.now()).getSeconds() < 900;
    }
}
