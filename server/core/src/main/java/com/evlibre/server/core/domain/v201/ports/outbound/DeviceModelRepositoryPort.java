package com.evlibre.server.core.domain.v201.ports.outbound;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;

import java.util.List;

/**
 * Stores the spec-shaped OCPP 2.0.1 Device Model as reported by a station
 * (via {@code NotifyReport} / {@code GetBaseReport}). Entries are keyed per
 * tenant + station identity; upserts are idempotent per
 * (component, variable) locator — replaying the same report re-applies the
 * same rows rather than appending duplicates.
 */
public interface DeviceModelRepositoryPort {

    /**
     * Upsert the given reports. For each entry, the (component, variable)
     * locator identifies a unique row; an existing row with the same locator
     * is overwritten, others are left untouched.
     */
    void upsert(TenantId tenantId,
                ChargePointIdentity stationIdentity,
                List<ReportedVariable> reports);

    /** All reports stored for this station, in no particular order. */
    List<ReportedVariable> findAll(TenantId tenantId, ChargePointIdentity stationIdentity);
}
