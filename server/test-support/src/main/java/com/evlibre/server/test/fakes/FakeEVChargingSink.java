package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.NotifyEVChargingNeedsStatus;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingNeeds;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingNeedsUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyEVChargingScheduleUseCaseV201;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combined fake policy/sink for the two ISO 15118 inbound messages. Captures
 * every call and returns a pre-configured status so tests can assert both the
 * decoded payload and the chosen response in one place.
 */
public class FakeEVChargingSink
        implements HandleNotifyEVChargingNeedsUseCaseV201.Policy,
                   HandleNotifyEVChargingScheduleUseCaseV201.Policy {

    public record NeedsEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            int evseId,
            Integer maxScheduleTuples,
            ChargingNeeds chargingNeeds) {}

    public record ScheduleEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Instant timeBase,
            int evseId,
            ChargingSchedule chargingSchedule) {}

    private final List<NeedsEvent> needsEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<ScheduleEvent> scheduleEvents = Collections.synchronizedList(new ArrayList<>());

    private volatile NotifyEVChargingNeedsStatus nextNeedsStatus = NotifyEVChargingNeedsStatus.ACCEPTED;
    private volatile GenericStatus nextScheduleStatus = GenericStatus.ACCEPTED;

    public void setNextNeedsStatus(NotifyEVChargingNeedsStatus status) {
        this.nextNeedsStatus = status;
    }

    public void setNextScheduleStatus(GenericStatus status) {
        this.nextScheduleStatus = status;
    }

    @Override
    public NotifyEVChargingNeedsStatus decide(TenantId tenantId,
                                              ChargePointIdentity stationIdentity,
                                              int evseId,
                                              Integer maxScheduleTuples,
                                              ChargingNeeds chargingNeeds) {
        needsEvents.add(new NeedsEvent(tenantId, stationIdentity, evseId, maxScheduleTuples, chargingNeeds));
        return nextNeedsStatus;
    }

    @Override
    public GenericStatus decide(TenantId tenantId,
                                ChargePointIdentity stationIdentity,
                                Instant timeBase,
                                int evseId,
                                ChargingSchedule chargingSchedule) {
        scheduleEvents.add(new ScheduleEvent(tenantId, stationIdentity, timeBase, evseId, chargingSchedule));
        return nextScheduleStatus;
    }

    public List<NeedsEvent> needsEvents() {
        return List.copyOf(needsEvents);
    }

    public List<ScheduleEvent> scheduleEvents() {
        return List.copyOf(scheduleEvents);
    }

    public void clear() {
        needsEvents.clear();
        scheduleEvents.clear();
    }
}
