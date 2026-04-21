package com.evlibre.server.core.usecases.v16;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.dto.CommandResult;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import com.evlibre.server.core.domain.shared.model.ChargingStation;
import com.evlibre.server.core.domain.shared.model.RegistrationStatus;
import com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort;

class RemoteStartTransactionUseCaseTest {

    private StubCommandSender commandSender;
    private RemoteStartTransactionUseCase useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
        useCase = new RemoteStartTransactionUseCase(commandSender);
    }

    @Test
    void sends_with_idTag_and_connectorId() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CommandResult result = useCase.remoteStart(tenantId, station, "TAG001", 1).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("RemoteStartTransaction");
        assertThat(cmd.payload()).containsEntry("idTag", "TAG001");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
    }

    @Test
    void sends_without_connectorId_when_null() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.remoteStart(tenantId, station, "TAG001", null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).doesNotContainKey("connectorId");
        assertThat(cmd.payload()).containsEntry("idTag", "TAG001");
    }

    // OCPP 1.6 §5.15: RemoteStartTransaction.chargingProfile MUST have purpose=TxProfile.
    @Test
    void rejects_chargingProfile_with_wrong_purpose() {
        Map<String, Object> profile = Map.of(
                "chargingProfileId", 1,
                "stackLevel", 0,
                "chargingProfilePurpose", "TxDefaultProfile",
                "chargingProfileKind", "Absolute");

        var future = useCase.remoteStart(tenantId, station, "TAG001", 1, profile);

        assertThat(future).isCompletedExceptionally();
        assertThat(commandSender.commands()).isEmpty();
    }

    // OCPP 1.6 §4.2: CSMS SHALL NOT send RemoteStart while station registration is Pending.
    @Test
    void rejects_when_station_registration_is_pending() {
        var stationRepo = new StubStationRepo();
        stationRepo.putRegistration(tenantId, station,
                com.evlibre.server.core.domain.shared.model.RegistrationStatus.PENDING);
        var gated = new RemoteStartTransactionUseCase(commandSender, stationRepo);

        var future = gated.remoteStart(tenantId, station, "TAG001", 1);

        assertThat(future).isCompletedExceptionally();
        assertThat(commandSender.commands()).isEmpty();
    }

    @Test
    void accepts_chargingProfile_with_TxProfile_purpose() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        Map<String, Object> profile = Map.of(
                "chargingProfileId", 1,
                "stackLevel", 0,
                "chargingProfilePurpose", "TxProfile",
                "chargingProfileKind", "Absolute");

        CommandResult result = useCase.remoteStart(tenantId, station, "TAG001", 1, profile).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsKey("chargingProfile");
    }

    // --- Fakes ---

    static class StubStationRepo implements com.evlibre.server.core.domain.shared.ports.outbound.StationRepositoryPort {
        private final java.util.Map<String, com.evlibre.server.core.domain.shared.model.ChargingStation> byIdentity = new java.util.HashMap<>();

        void putRegistration(TenantId t, ChargePointIdentity id,
                              com.evlibre.server.core.domain.shared.model.RegistrationStatus status) {
            byIdentity.put(t.value() + ":" + id.value(),
                    com.evlibre.server.core.domain.shared.model.ChargingStation.builder()
                            .id(java.util.UUID.randomUUID())
                            .tenantId(t)
                            .identity(id)
                            .protocol(com.evlibre.common.ocpp.OcppProtocol.OCPP_16)
                            .vendor("V").model("M")
                            .registrationStatus(status)
                            .createdAt(java.time.Instant.now())
                            .build());
        }

        @Override public void save(com.evlibre.server.core.domain.shared.model.ChargingStation s) {
            byIdentity.put(s.tenantId().value() + ":" + s.identity().value(), s);
        }
        @Override public java.util.Optional<com.evlibre.server.core.domain.shared.model.ChargingStation> findById(java.util.UUID id) {
            return byIdentity.values().stream().filter(s -> s.id().equals(id)).findFirst();
        }
        @Override public java.util.Optional<com.evlibre.server.core.domain.shared.model.ChargingStation> findByTenantAndIdentity(
                TenantId t, ChargePointIdentity id) {
            return java.util.Optional.ofNullable(byIdentity.get(t.value() + ":" + id.value()));
        }
        @Override public java.util.List<com.evlibre.server.core.domain.shared.model.ChargingStation> findByTenant(TenantId t) {
            return byIdentity.values().stream().filter(s -> s.tenantId().equals(t)).toList();
        }
    }
}
