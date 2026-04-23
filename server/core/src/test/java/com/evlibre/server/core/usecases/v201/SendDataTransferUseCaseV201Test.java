package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DataTransferResult;
import com.evlibre.server.core.domain.v201.dto.DataTransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendDataTransferUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private SendDataTransferUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new SendDataTransferUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void vendorId_only_builds_minimal_payload() {
        useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, null).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("DataTransfer");
        assertThat(cmd.payload()).containsOnlyKeys("vendorId");
        assertThat(cmd.payload().get("vendorId")).isEqualTo("com.evlibre.probe");
    }

    @Test
    void optional_fields_included_when_present() {
        useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", "ping",
                Map.of("k", 1)).join();

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.payload()).containsEntry("vendorId", "com.evlibre.probe");
        assertThat(cmd.payload()).containsEntry("messageId", "ping");
        assertThat(cmd.payload()).containsEntry("data", Map.of("k", 1));
    }

    // 2.0.1 `data` is anyType — verify the use case passes through primitives, lists, maps
    // unchanged without any serialization step in the domain layer.
    @Test
    void data_accepts_primitives_and_collections() {
        useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, "raw-string").join();
        useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, 42).join();
        useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, List.of("a", "b")).join();

        var payloads = commandSender.commands().stream().map(c -> c.payload().get("data")).toList();
        assertThat(payloads).containsExactly("raw-string", 42, List.of("a", "b"));
    }

    @Test
    void accepted_status_parsed() {
        DataTransferResult r = useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, null).join();

        assertThat(r.status()).isEqualTo(DataTransferStatus.ACCEPTED);
        assertThat(r.isAccepted()).isTrue();
    }

    @Test
    void rejected_with_statusInfo_reason_parsed() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "InvalidPayload")));

        DataTransferResult r = useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, null).join();

        assertThat(r.status()).isEqualTo(DataTransferStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("InvalidPayload");
        assertThat(r.isAccepted()).isFalse();
    }

    @Test
    void unknown_messageId_and_vendorId_both_supported() {
        commandSender.setNextResponse(Map.of("status", "UnknownMessageId"));
        assertThat(useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", "unknown", null).join().status())
                .isEqualTo(DataTransferStatus.UNKNOWN_MESSAGE_ID);

        commandSender.setNextResponse(Map.of("status", "UnknownVendorId"));
        assertThat(useCase.sendDataTransfer(tenantId, station, "com.nope", null, null).join().status())
                .isEqualTo(DataTransferStatus.UNKNOWN_VENDOR_ID);
    }

    @Test
    void response_data_returned_as_object_tree() {
        commandSender.setNextResponse(Map.of(
                "status", "Accepted",
                "data", Map.of("echo", "pong", "count", 3)));

        DataTransferResult r = useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, null).join();

        assertThat(r.data()).isEqualTo(Map.of("echo", "pong", "count", 3));
    }

    @Test
    void unknown_wire_status_rejected() {
        commandSender.setNextResponse(Map.of("status", "Maybe"));

        assertThatThrownBy(() ->
                useCase.sendDataTransfer(tenantId, station, "com.evlibre.probe", null, null).join())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maybe");
    }

    @Test
    void null_vendorId_rejected() {
        assertThatThrownBy(() ->
                useCase.sendDataTransfer(tenantId, station, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
