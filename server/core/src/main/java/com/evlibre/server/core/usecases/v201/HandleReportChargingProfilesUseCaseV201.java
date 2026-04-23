package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleReportChargingProfilesPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Pass-through handler for {@code ReportChargingProfilesRequest}. Logs each
 * frame and forwards it to a consumer port so a subscriber (UI, audit log, a
 * future {@code ChargingProfileRepositoryPort}) can capture the data.
 *
 * <p>Per K09.FR.01 / K09.FR.02 the CSMS is expected to correlate frames with
 * their originating {@code GetChargingProfilesRequest} via {@code requestId}
 * and to treat {@code tbc=false} as the last frame — this use case leaves both
 * concerns to the consumer, which keeps it usable from simple read-only sinks
 * and from stateful aggregators alike.
 */
public class HandleReportChargingProfilesUseCaseV201 implements HandleReportChargingProfilesPort {

    /** Consumer signature for callers that want to observe reports without implementing the port. */
    @FunctionalInterface
    public interface Sink {
        void onFrame(TenantId tenantId,
                     ChargePointIdentity stationIdentity,
                     int requestId,
                     ChargingLimitSource source,
                     int evseId,
                     List<ChargingProfile> profiles,
                     boolean tbc);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleReportChargingProfilesUseCaseV201.class);

    private final Sink sink;

    public HandleReportChargingProfilesUseCaseV201(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void handleFrame(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int requestId,
            ChargingLimitSource source,
            int evseId,
            List<ChargingProfile> profiles,
            boolean tbc) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(profiles, "profiles");
        log.info("ReportChargingProfiles frame from {} (requestId={}, source={}, evseId={}, profiles={}, tbc={})",
                stationIdentity.value(), requestId, source, evseId, profiles.size(), tbc);
        sink.onFrame(tenantId, stationIdentity, requestId, source, evseId, profiles, tbc);
    }
}
