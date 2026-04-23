package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.dto.TriggerMessageResult;
import com.evlibre.server.core.domain.v201.dto.TriggerMessageStatus;
import com.evlibre.server.core.domain.v201.model.MessageTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriggerMessageUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private TriggerMessageUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new TriggerMessageUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void whole_station_trigger_omits_evse() {
        useCase.triggerMessage(tenantId, station, MessageTrigger.HEARTBEAT, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("TriggerMessage");
        assertThat(cmd.payload())
                .containsEntry("requestedMessage", "Heartbeat")
                .doesNotContainKey("evse");
    }

    @Test
    void evse_level_trigger_serialises_id_only() {
        useCase.triggerMessage(tenantId, station, MessageTrigger.METER_VALUES, Evse.of(2)).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("evse");
        assertThat(evse).containsOnly(Map.entry("id", 2));
    }

    @Test
    void connector_level_trigger_serialises_id_and_connector() {
        useCase.triggerMessage(tenantId, station,
                MessageTrigger.STATUS_NOTIFICATION, Evse.of(1, 2)).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> evse = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("evse");
        assertThat(evse)
                .containsEntry("id", 1)
                .containsEntry("connectorId", 2);
    }

    @Test
    void status_notification_without_connector_rejected() {
        assertThatThrownBy(() -> useCase.triggerMessage(tenantId, station,
                MessageTrigger.STATUS_NOTIFICATION, Evse.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
        assertThatThrownBy(() -> useCase.triggerMessage(tenantId, station,
                MessageTrigger.STATUS_NOTIFICATION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectorId");
    }

    @Test
    void all_trigger_enum_values_have_distinct_wire_mapping() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MessageTrigger t : MessageTrigger.values()) {
            commandSender = new StubCommandSender201();
            commandSender.setNextResponse(Map.of("status", "Accepted"));
            useCase = new TriggerMessageUseCaseV201(commandSender);
            // StatusNotification needs connectorId; others don't
            Evse evse = (t == MessageTrigger.STATUS_NOTIFICATION) ? Evse.of(1, 1) : null;

            useCase.triggerMessage(tenantId, station, t, evse).join();

            String wire = (String) commandSender.commands().get(0).payload().get("requestedMessage");
            assertThat(seen.add(wire))
                    .as("duplicate wire value %s for enum %s", wire, t)
                    .isTrue();
            assertThat(wire).isNotEmpty();
        }
        assertThat(seen).hasSize(MessageTrigger.values().length);
    }

    @Test
    void accepted_status_parsed() {
        TriggerMessageResult r = useCase.triggerMessage(
                tenantId, station, MessageTrigger.BOOT_NOTIFICATION, null).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(TriggerMessageStatus.ACCEPTED);
    }

    @Test
    void not_implemented_status_parsed() {
        commandSender.setNextResponse(Map.of("status", "NotImplemented"));

        TriggerMessageResult r = useCase.triggerMessage(
                tenantId, station, MessageTrigger.SIGN_V2G_CERTIFICATE, null).join();

        assertThat(r.status()).isEqualTo(TriggerMessageStatus.NOT_IMPLEMENTED);
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void rejected_surfaces_status_info_reason() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "BadState")));

        TriggerMessageResult r = useCase.triggerMessage(
                tenantId, station, MessageTrigger.METER_VALUES, null).join();

        assertThat(r.status()).isEqualTo(TriggerMessageStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("BadState");
    }

    @Test
    void unknown_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Pending"));

        assertThatThrownBy(() -> useCase.triggerMessage(
                tenantId, station, MessageTrigger.HEARTBEAT, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pending");
    }

    @Test
    void null_requested_message_rejected() {
        assertThatThrownBy(() -> useCase.triggerMessage(tenantId, station, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("requestedMessage");
    }
}
