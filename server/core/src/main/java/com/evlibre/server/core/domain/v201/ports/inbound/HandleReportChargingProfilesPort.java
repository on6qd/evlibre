package com.evlibre.server.core.domain.v201.ports.inbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;

import java.util.List;

/**
 * Inbound port for {@code ReportChargingProfilesRequest} (K09 follow-up). The
 * station calls this once per frame in response to an earlier
 * {@code GetChargingProfilesRequest}; {@code requestId} correlates the frames
 * back to the original CSMS-initiated call, and {@code tbc} signals whether
 * more frames follow (K09.FR.02).
 */
public interface HandleReportChargingProfilesPort {

    void handleFrame(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            ChargingLimitSource source,
            int evseId,
            List<ChargingProfile> profiles,
            boolean tbc);
}
