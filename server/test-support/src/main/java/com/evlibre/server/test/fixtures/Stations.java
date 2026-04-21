package com.evlibre.server.test.fixtures;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.model.TenantId;

import java.time.Instant;
import java.util.UUID;

public final class Stations {

    public static final ChargePointIdentity DEFAULT_IDENTITY = new ChargePointIdentity("CHARGER-001");
    public static final Instant DEFAULT_TIME = Instant.parse("2025-01-15T10:00:00Z");

    private Stations() {}

    public static ChargingStation accepted(TenantId tenantId) {
        return builder(tenantId)
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .build();
    }

    public static ChargingStation accepted(TenantId tenantId, String identity) {
        return builder(tenantId)
                .identity(new ChargePointIdentity(identity))
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .build();
    }

    public static ChargingStation.Builder builder(TenantId tenantId) {
        return ChargingStation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .identity(DEFAULT_IDENTITY)
                .protocol(OcppProtocol.OCPP_16)
                .vendor("ABB")
                .model("Terra AC")
                .registrationStatus(RegistrationStatus.ACCEPTED)
                .createdAt(DEFAULT_TIME);
    }
}
