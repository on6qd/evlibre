package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyEVChargingNeedsPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingNeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default {@code NotifyEVChargingNeeds} use case. Logs the incoming needs and
 * delegates the decision to a {@link Policy} — caller-supplied so the CSMS can
 * plug in whatever smart-charging logic (or absence thereof) it currently has.
 * The default policy in {@code Application} returns {@link
 * NotifyEVChargingNeedsStatus#ACCEPTED} so stations aren't blocked while real
 * schedule synthesis is still in Phase 5+.
 */
public class HandleNotifyEVChargingNeedsUseCaseV201 implements HandleNotifyEVChargingNeedsPort {

    @FunctionalInterface
    public interface Policy {
        NotifyEVChargingNeedsStatus decide(
                TenantId tenantId,
                ChargePointIdentity stationIdentity,
                int evseId,
                Integer maxScheduleTuples,
                ChargingNeeds chargingNeeds);
    }

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyEVChargingNeedsUseCaseV201.class);

    private final Policy policy;

    public HandleNotifyEVChargingNeedsUseCaseV201(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public NotifyEVChargingNeedsStatus handleNotifyEVChargingNeeds(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            Integer maxScheduleTuples,
            ChargingNeeds chargingNeeds) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(chargingNeeds, "chargingNeeds");
        log.info("NotifyEVChargingNeeds from {} (evseId={}, mode={}, departureTime={}, maxScheduleTuples={})",
                stationIdentity.value(), evseId,
                chargingNeeds.requestedEnergyTransfer(), chargingNeeds.departureTime(),
                maxScheduleTuples);
        return policy.decide(tenantId, stationIdentity, evseId, maxScheduleTuples, chargingNeeds);
    }
}
