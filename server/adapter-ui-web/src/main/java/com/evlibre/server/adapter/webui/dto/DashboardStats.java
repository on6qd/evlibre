package com.evlibre.server.adapter.webui.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.model.ChargingStation;

import java.util.List;
import java.util.Set;

public record DashboardStats(
        int totalStations,
        int onlineStations,
        int offlineStations,
        double uptimePercentage
) {

    public static DashboardStats calculate(List<ChargingStation> stations,
                                           Set<ChargePointIdentity> connectedStations) {
        int online = (int) stations.stream()
                .filter(s -> connectedStations.contains(s.identity()))
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
}
