package com.evlibre.server.test.fakes;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimit;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingSchedule;
import com.evlibre.server.core.usecases.v201.HandleClearedChargingLimitUseCaseV201;
import com.evlibre.server.core.usecases.v201.HandleNotifyChargingLimitUseCaseV201;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combined sink for the two inbound charging-limit messages. One type lets
 * tests observe both sides of the limit lifecycle without having to juggle two
 * fakes.
 */
public class FakeChargingLimitSink
        implements HandleNotifyChargingLimitUseCaseV201.Sink,
                   HandleClearedChargingLimitUseCaseV201.Sink {

    public record NotifyEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            Integer evseId,
            ChargingLimit chargingLimit,
            List<ChargingSchedule> schedules) {}

    public record ClearedEvent(
            TenantId tenantId,
            ChargePointIdentity stationIdentity,
            ChargingLimitSource source,
            Integer evseId) {}

    private final List<NotifyEvent> notifyEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<ClearedEvent> clearedEvents = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onNotify(TenantId tenantId,
                         ChargePointIdentity stationIdentity,
                         Integer evseId,
                         ChargingLimit chargingLimit,
                         List<ChargingSchedule> schedules) {
        notifyEvents.add(new NotifyEvent(tenantId, stationIdentity, evseId, chargingLimit, schedules));
    }

    @Override
    public void onCleared(TenantId tenantId,
                          ChargePointIdentity stationIdentity,
                          ChargingLimitSource source,
                          Integer evseId) {
        clearedEvents.add(new ClearedEvent(tenantId, stationIdentity, source, evseId));
    }

    public List<NotifyEvent> notifyEvents() {
        return List.copyOf(notifyEvents);
    }

    public List<ClearedEvent> clearedEvents() {
        return List.copyOf(clearedEvents);
    }

    public void clear() {
        notifyEvents.clear();
        clearedEvents.clear();
    }
}
