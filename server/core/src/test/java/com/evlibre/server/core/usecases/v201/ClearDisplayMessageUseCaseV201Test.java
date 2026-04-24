package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.ClearDisplayMessageResult;
import com.evlibre.server.core.domain.v201.displaymessage.ClearMessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClearDisplayMessageUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private ClearDisplayMessageUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new ClearDisplayMessageUseCaseV201(commandSender);
    }

    @Test
    void id_serialised_on_wire() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.clearDisplayMessage(tenantId, station, 42).join();

        assertThat(commandSender.commands().get(0).payload()).containsEntry("id", 42);
    }

    @Test
    void both_response_statuses_decoded() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));
        assertThat(useCase.clearDisplayMessage(tenantId, station, 1).join().status())
                .isEqualTo(ClearMessageStatus.ACCEPTED);

        commandSender.setNextResponse(Map.of("status", "Unknown"));
        ClearDisplayMessageResult r = useCase.clearDisplayMessage(tenantId, station, 1).join();
        assertThat(r.status()).isEqualTo(ClearMessageStatus.UNKNOWN);
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void statusInfo_reasonCode_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Unknown",
                "statusInfo", Map.of("reasonCode", "MessageNotFound")));

        ClearDisplayMessageResult r = useCase.clearDisplayMessage(tenantId, station, 1).join();

        assertThat(r.statusInfoReason()).isEqualTo("MessageNotFound");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.clearDisplayMessage(tenantId, station, 1).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
