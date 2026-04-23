package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.GenericStatus;
import com.evlibre.server.core.domain.v201.dto.PublishFirmwareResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishFirmwareUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private PublishFirmwareUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");
    private static final String VALID_MD5 = "0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new PublishFirmwareUseCaseV201(commandSender);
        commandSender.setNextResponse(Map.of("status", "Accepted"));
    }

    @Test
    void minimal_payload_omits_retries_and_retryInterval() {
        PublishFirmwareResult r = useCase.publishFirmware(
                tenantId, station, 51,
                "https://csms/fw.bin", VALID_MD5, null, null).join();

        assertThat(r.isAccepted()).isTrue();
        assertThat(r.status()).isEqualTo(GenericStatus.ACCEPTED);

        var cmd = commandSender.commands().get(0);
        assertThat(cmd.action()).isEqualTo("PublishFirmware");
        assertThat(cmd.payload())
                .containsEntry("location", "https://csms/fw.bin")
                .containsEntry("checksum", VALID_MD5)
                .containsEntry("requestId", 51)
                .doesNotContainKey("retries")
                .doesNotContainKey("retryInterval");
    }

    @Test
    void full_payload_includes_retries_and_retryInterval() {
        useCase.publishFirmware(tenantId, station, 52,
                "https://csms/fw.bin", VALID_MD5, 3, 60).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("retries", 3)
                .containsEntry("retryInterval", 60);
    }

    @Test
    void uppercase_hex_checksum_accepted() {
        useCase.publishFirmware(tenantId, station, 53,
                "https://csms/fw.bin", "ABCDEF0123456789ABCDEF0123456789", null, null).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("checksum", "ABCDEF0123456789ABCDEF0123456789");
    }

    @Test
    void rejects_non_hex_checksum() {
        assertThatThrownBy(() -> useCase.publishFirmware(
                tenantId, station, 54,
                "https://csms/fw.bin",
                "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ",
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MD5 hex");
    }

    @Test
    void rejects_wrong_length_checksum() {
        assertThatThrownBy(() -> useCase.publishFirmware(
                tenantId, station, 55,
                "https://csms/fw.bin", "abc", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MD5 hex");
    }

    @Test
    void rejects_blank_location() {
        assertThatThrownBy(() -> useCase.publishFirmware(
                tenantId, station, 56, "", VALID_MD5, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("location");
    }

    @Test
    void rejects_negative_retries() {
        assertThatThrownBy(() -> useCase.publishFirmware(
                tenantId, station, 57,
                "https://csms/fw.bin", VALID_MD5, -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retries");
    }

    @Test
    void rejected_status_surfaces_reason_code() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "DiskFull")));

        PublishFirmwareResult r = useCase.publishFirmware(
                tenantId, station, 58,
                "https://csms/fw.bin", VALID_MD5, null, null).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(GenericStatus.REJECTED);
        assertThat(r.statusInfoReason()).isEqualTo("DiskFull");
    }
}
