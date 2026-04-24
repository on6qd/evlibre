package com.evlibre.server.core.domain.v201.dto;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.model.EvseId;
import com.evlibre.common.model.MeterValue;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.util.List;
import java.util.Objects;

public record MeterValuesData201(
        TenantId tenantId,
        ChargePointIdentity stationIdentity,
        EvseId evseId,
        List<MeterValue> meterValues
) {
    public MeterValuesData201 {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(stationIdentity, "stationIdentity");
        Objects.requireNonNull(evseId, "evseId");
        Objects.requireNonNull(meterValues, "meterValues");
        if (meterValues.isEmpty()) {
            throw new IllegalArgumentException("meterValues must contain at least one entry");
        }
        meterValues = List.copyOf(meterValues);
    }
}
