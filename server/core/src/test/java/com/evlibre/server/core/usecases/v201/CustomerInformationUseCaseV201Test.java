package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationResult;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationStatus;
import com.evlibre.server.core.domain.v201.dto.CustomerInformationTarget;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.domain.v201.security.CertificateHashData;
import com.evlibre.server.core.domain.v201.security.HashAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerInformationUseCaseV201Test {

    private StubCommandSender201 commandSender;
    private CustomerInformationUseCaseV201 useCase;

    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CHARGER-001");

    @BeforeEach
    void setUp() {
        commandSender = new StubCommandSender201();
        useCase = new CustomerInformationUseCaseV201(commandSender);
    }

    @Test
    void customerIdentifier_over_64_chars_is_rejected_at_construction() {
        String tooLong = "x".repeat(65);

        assertThatThrownBy(() -> CustomerInformationTarget.byIdentifier(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64");
    }

    @Test
    void null_target_rejected() {
        assertThatThrownBy(() -> useCase.customerInformation(tenantId, station, 1, true, false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("target");
    }

    @Test
    void empty_target_emits_only_required_fields() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.customerInformation(tenantId, station, 42, true, false,
                CustomerInformationTarget.none()).join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsEntry("requestId", 42)
                .containsEntry("report", true)
                .containsEntry("clear", false)
                .doesNotContainKey("customerIdentifier")
                .doesNotContainKey("idToken")
                .doesNotContainKey("customerCertificate");
    }

    @Test
    void byIdentifier_serialises_customerIdentifier_field() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.customerInformation(tenantId, station, 1, true, false,
                CustomerInformationTarget.byIdentifier("CUST-001")).join();

        assertThat(commandSender.commands().get(0).payload())
                .containsEntry("customerIdentifier", "CUST-001");
    }

    @Test
    void byIdToken_serialises_nested_idToken_object() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        useCase.customerInformation(tenantId, station, 1, false, true,
                CustomerInformationTarget.byIdToken(IdToken.of("TAG-123", IdTokenType.ISO14443))).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> idTokenWire = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("idToken");
        assertThat(idTokenWire)
                .containsEntry("idToken", "TAG-123")
                .containsEntry("type", "ISO14443");
    }

    @Test
    void byCertificate_serialises_nested_customerCertificate_hash_tuple() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CertificateHashData cert = new CertificateHashData(
                HashAlgorithm.SHA256, "issuerName", "issuerKey", "serial");

        useCase.customerInformation(tenantId, station, 1, true, false,
                CustomerInformationTarget.byCertificate(cert)).join();

        @SuppressWarnings("unchecked")
        Map<String, Object> certWire = (Map<String, Object>) commandSender.commands().get(0)
                .payload().get("customerCertificate");
        assertThat(certWire)
                .containsEntry("hashAlgorithm", "SHA256")
                .containsEntry("issuerNameHash", "issuerName")
                .containsEntry("issuerKeyHash", "issuerKey")
                .containsEntry("serialNumber", "serial");
    }

    @Test
    void all_three_identifiers_together_all_serialised() {
        commandSender.setNextResponse(Map.of("status", "Accepted"));

        CertificateHashData cert = new CertificateHashData(HashAlgorithm.SHA256, "n", "k", "s");
        IdToken token = IdToken.of("TAG", IdTokenType.CENTRAL);

        useCase.customerInformation(tenantId, station, 1, true, true,
                new CustomerInformationTarget("CUST", token, cert)).join();

        Map<String, Object> payload = commandSender.commands().get(0).payload();
        assertThat(payload)
                .containsKey("customerIdentifier")
                .containsKey("idToken")
                .containsKey("customerCertificate");
    }

    @Test
    void all_three_response_statuses_decoded() {
        for (String wire : new String[] {"Accepted", "Rejected", "Invalid"}) {
            commandSender.setNextResponse(Map.of("status", wire));
            CustomerInformationResult r = useCase.customerInformation(tenantId, station, 1,
                    true, false, CustomerInformationTarget.none()).join();
            assertThat(r.status().name()).isEqualTo(wire.toUpperCase());
        }
    }

    @Test
    void invalid_status_is_not_accepted() {
        commandSender.setNextResponse(Map.of("status", "Invalid"));

        CustomerInformationResult r = useCase.customerInformation(tenantId, station, 1,
                true, false, CustomerInformationTarget.none()).join();

        assertThat(r.isAccepted()).isFalse();
        assertThat(r.status()).isEqualTo(CustomerInformationStatus.INVALID);
    }

    @Test
    void statusInfo_reasonCode_surfaced() {
        commandSender.setNextResponse(Map.of(
                "status", "Rejected",
                "statusInfo", Map.of("reasonCode", "UnknownCustomer")));

        CustomerInformationResult r = useCase.customerInformation(tenantId, station, 1,
                true, false, CustomerInformationTarget.none()).join();

        assertThat(r.statusInfoReason()).isEqualTo("UnknownCustomer");
    }

    @Test
    void unknown_wire_status_raises() {
        commandSender.setNextResponse(Map.of("status", "FutureStatus"));

        assertThatThrownBy(() -> useCase.customerInformation(tenantId, station, 1,
                true, false, CustomerInformationTarget.none()).join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
