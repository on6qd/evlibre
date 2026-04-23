package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.usecases.v201.HandleReportChargingProfilesUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeReportChargingProfilesSink implements HandleReportChargingProfilesUseCaseV201.Sink {

    public record Frame(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            ChargingLimitSource source,
            int evseId,
            List<ChargingProfile> profiles,
            boolean tbc) {}

    private final List<Frame> frames = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onFrame(TenantId tenantId,
                        ChargePointIdentity stationIdentity,
                        int requestId,
                        ChargingLimitSource source,
                        int evseId,
                        List<ChargingProfile> profiles,
                        boolean tbc) {
        frames.add(new Frame(tenantId, stationIdentity, requestId, source, evseId, profiles, tbc));
    }

    public List<Frame> frames() {
        return List.copyOf(frames);
    }

    public void clear() {
        frames.clear();
    }
}
