package com.evlibre.server.core.usecases;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.dto.CommandResult;
import com.evlibre.server.core.domain.model.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmartChargingUseCaseTest {

    private StubCommandSender commandSender;
    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender();
    }

    @Test
    void setChargingProfile_sends_profile() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        var useCase = new SetChargingProfileUseCase(commandSender);

        Map<String, Object> profile = Map.of(
                "chargingProfileId", 1,
                "stackLevel", 0,
                "chargingProfilePurpose", "TxDefaultProfile",
                "chargingProfileKind", "Absolute"
        );

        CommandResult result = useCase.setChargingProfile(tenantId, station, 1, profile).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("SetChargingProfile");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
        assertThat(cmd.payload()).containsKey("csChargingProfiles");
    }

    @Test
    void clearChargingProfile_sends_filters() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        var useCase = new ClearChargingProfileUseCase(commandSender);

        CommandResult result = useCase.clearChargingProfile(tenantId, station,
                null, 1, "TxDefaultProfile", null).join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("ClearChargingProfile");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
        assertThat(cmd.payload()).containsEntry("chargingProfilePurpose", "TxDefaultProfile");
        assertThat(cmd.payload()).doesNotContainKey("id");
    }

    @Test
    void getCompositeSchedule_sends_params() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        var useCase = new GetCompositeScheduleUseCase(commandSender);

        CommandResult result = useCase.getCompositeSchedule(tenantId, station, 1, 3600, "W").join();

        assertThat(result.isAccepted()).isTrue();
        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("GetCompositeSchedule");
        assertThat(cmd.payload()).containsEntry("connectorId", 1);
        assertThat(cmd.payload()).containsEntry("duration", 3600);
        assertThat(cmd.payload()).containsEntry("chargingRateUnit", "W");
    }
}
