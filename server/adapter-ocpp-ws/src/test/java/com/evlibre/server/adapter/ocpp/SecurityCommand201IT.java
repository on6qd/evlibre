package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestClient;
import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.dto.DeleteCertificateStatus;
import com.evlibre.server.core.domain.v201.dto.GetInstalledCertificateIdsStatus;
import com.evlibre.server.core.domain.v201.dto.InstallCertificateStatus;
import com.evlibre.server.core.domain.v201.security.CertificateHashData;
import com.evlibre.server.core.domain.v201.security.GetCertificateIdUse;
import com.evlibre.server.core.domain.v201.security.HashAlgorithm;
import com.evlibre.server.core.domain.v201.security.InstallCertificateUse;
import com.evlibre.server.core.usecases.v201.DeleteCertificateUseCaseV201;
import com.evlibre.server.core.usecases.v201.GetInstalledCertificateIdsUseCaseV201;
import com.evlibre.server.core.usecases.v201.InstallCertificateUseCaseV201;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSMS-to-CS v2.0.1 security/certificate commands
 * (block M — Certificate Management) over a real WebSocket connection. Both
 * directions are schema-validated end-to-end by OcppSchemaValidator
 * (hard-reject mode).
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class SecurityCommand201IT {

    private OcppTestHarness harness;
    private static final TenantId TENANT = new TenantId("demo-tenant");
    private static final ChargePointIdentity STATION = new ChargePointIdentity("SEC-CMD-201");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void get_installed_certificate_ids_all_types_returns_chain(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetInstalledCertificateIds", payload -> Map.of(
                            "status", "Accepted",
                            "certificateHashDataChain", List.of(
                                    Map.of(
                                            "certificateType", "CSMSRootCertificate",
                                            "certificateHashData", Map.of(
                                                    "hashAlgorithm", "SHA256",
                                                    "issuerNameHash", "aaaaaaaabbbbbbbbccccccccdddddddd",
                                                    "issuerKeyHash", "1111111122222222333333334444444455",
                                                    "serialNumber", "0A1B2C3D"
                                            )
                                    ),
                                    Map.of(
                                            "certificateType", "V2GCertificateChain",
                                            "certificateHashData", Map.of(
                                                    "hashAlgorithm", "SHA384",
                                                    "issuerNameHash", "v2g-root-name-hash",
                                                    "issuerKeyHash", "v2g-root-key-hash",
                                                    "serialNumber", "FF00"
                                            ),
                                            "childCertificateHashData", List.of(
                                                    Map.of(
                                                            "hashAlgorithm", "SHA384",
                                                            "issuerNameHash", "v2g-leaf-name-hash",
                                                            "issuerKeyHash", "v2g-leaf-key-hash",
                                                            "serialNumber", "FF01"
                                                    )
                                            )
                                    )
                            )));
                    GetInstalledCertificateIdsUseCaseV201 useCase =
                            new GetInstalledCertificateIdsUseCaseV201(harness.commandSender201);
                    return useCase.getInstalledCertificateIds(TENANT, STATION, null)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status())
                                            .isEqualTo(GetInstalledCertificateIdsStatus.ACCEPTED);
                                    assertThat(result.certificateHashDataChain()).hasSize(2);

                                    var csms = result.certificateHashDataChain().get(0);
                                    assertThat(csms.certificateType())
                                            .isEqualTo(GetCertificateIdUse.CSMS_ROOT_CERTIFICATE);
                                    assertThat(csms.certificateHashData().hashAlgorithm())
                                            .isEqualTo(HashAlgorithm.SHA256);
                                    assertThat(csms.certificateHashData().serialNumber())
                                            .isEqualTo("0A1B2C3D");
                                    assertThat(csms.childCertificateHashData()).isEmpty();

                                    var v2g = result.certificateHashDataChain().get(1);
                                    assertThat(v2g.certificateType())
                                            .isEqualTo(GetCertificateIdUse.V2G_CERTIFICATE_CHAIN);
                                    assertThat(v2g.childCertificateHashData()).hasSize(1);
                                    assertThat(v2g.childCertificateHashData().get(0).serialNumber())
                                            .isEqualTo("FF01");

                                    // Without a filter, the request MUST NOT carry a certificateType.
                                    var cmd = client.receivedCommands("GetInstalledCertificateIds").get(0);
                                    assertThat(cmd.payload().has("certificateType")).isFalse();
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void install_certificate_csms_root_accepted(Vertx vertx, VertxTestContext ctx) {
        String pem = """
                -----BEGIN CERTIFICATE-----
                MIICljCCAX4CCQDkY7k9RhEcjzANBgkqhkiG9w0BAQsFADCBkTELMAkGA1UEBhMC
                -----END CERTIFICATE-----""";

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("InstallCertificate", "Accepted");
                    InstallCertificateUseCaseV201 useCase =
                            new InstallCertificateUseCaseV201(harness.commandSender201);
                    return useCase.installCertificate(TENANT, STATION,
                                    InstallCertificateUse.CSMS_ROOT_CERTIFICATE, pem)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status())
                                            .isEqualTo(InstallCertificateStatus.ACCEPTED);
                                    assertThat(result.statusInfoReason()).isNull();

                                    var cmd = client.receivedCommands("InstallCertificate").get(0);
                                    assertThat(cmd.payload().get("certificateType").asText())
                                            .isEqualTo("CSMSRootCertificate");
                                    assertThat(cmd.payload().get("certificate").asText())
                                            .contains("BEGIN CERTIFICATE");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void install_certificate_failed_surfaces_reason(Vertx vertx, VertxTestContext ctx) {
        String pem = """
                -----BEGIN CERTIFICATE-----
                invalid
                -----END CERTIFICATE-----""";

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("InstallCertificate", payload -> Map.of(
                            "status", "Failed",
                            "statusInfo", Map.of("reasonCode", "InvalidSignature")));
                    InstallCertificateUseCaseV201 useCase =
                            new InstallCertificateUseCaseV201(harness.commandSender201);
                    return useCase.installCertificate(TENANT, STATION,
                                    InstallCertificateUse.V2G_ROOT_CERTIFICATE, pem)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status())
                                            .isEqualTo(InstallCertificateStatus.FAILED);
                                    assertThat(result.statusInfoReason()).isEqualTo("InvalidSignature");

                                    var cmd = client.receivedCommands("InstallCertificate").get(0);
                                    assertThat(cmd.payload().get("certificateType").asText())
                                            .isEqualTo("V2GRootCertificate");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void delete_certificate_by_hash_accepted(Vertx vertx, VertxTestContext ctx) {
        CertificateHashData hash = new CertificateHashData(
                HashAlgorithm.SHA256,
                "name-hash-hex", "key-hash-hex", "0A1B2C3D");

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DeleteCertificate", "Accepted");
                    DeleteCertificateUseCaseV201 useCase =
                            new DeleteCertificateUseCaseV201(harness.commandSender201);
                    return useCase.deleteCertificate(TENANT, STATION, hash)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isTrue();
                                    assertThat(result.status())
                                            .isEqualTo(DeleteCertificateStatus.ACCEPTED);

                                    var cmd = client.receivedCommands("DeleteCertificate").get(0);
                                    var h = cmd.payload().get("certificateHashData");
                                    assertThat(h.get("hashAlgorithm").asText()).isEqualTo("SHA256");
                                    assertThat(h.get("serialNumber").asText()).isEqualTo("0A1B2C3D");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void delete_certificate_not_found(Vertx vertx, VertxTestContext ctx) {
        CertificateHashData hash = new CertificateHashData(
                HashAlgorithm.SHA384, "nh", "kh", "MISSING");

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DeleteCertificate", "NotFound");
                    DeleteCertificateUseCaseV201 useCase =
                            new DeleteCertificateUseCaseV201(harness.commandSender201);
                    return useCase.deleteCertificate(TENANT, STATION, hash)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status())
                                            .isEqualTo(DeleteCertificateStatus.NOT_FOUND);
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void delete_certificate_failed_with_reason(Vertx vertx, VertxTestContext ctx) {
        CertificateHashData hash = new CertificateHashData(
                HashAlgorithm.SHA256, "nh", "kh", "IN-USE");

        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("DeleteCertificate", payload -> Map.of(
                            "status", "Failed",
                            "statusInfo", Map.of("reasonCode", "CertInUse")));
                    DeleteCertificateUseCaseV201 useCase =
                            new DeleteCertificateUseCaseV201(harness.commandSender201);
                    return useCase.deleteCertificate(TENANT, STATION, hash)
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.status())
                                            .isEqualTo(DeleteCertificateStatus.FAILED);
                                    assertThat(result.statusInfoReason()).isEqualTo("CertInUse");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }

    @Test
    void get_installed_certificate_ids_filters_requested_types(Vertx vertx, VertxTestContext ctx) {
        OcppTestClient.connect(vertx, harness, STATION.value(), "ocpp2.0.1")
                .thenCompose(client -> {
                    client.onCommand("GetInstalledCertificateIds", payload -> Map.of(
                            "status", "NotFound"));
                    GetInstalledCertificateIdsUseCaseV201 useCase =
                            new GetInstalledCertificateIdsUseCaseV201(harness.commandSender201);
                    return useCase.getInstalledCertificateIds(TENANT, STATION,
                                    List.of(GetCertificateIdUse.MO_ROOT_CERTIFICATE,
                                            GetCertificateIdUse.MANUFACTURER_ROOT_CERTIFICATE))
                            .thenApply(result -> {
                                ctx.verify(() -> {
                                    assertThat(result.isAccepted()).isFalse();
                                    assertThat(result.status())
                                            .isEqualTo(GetInstalledCertificateIdsStatus.NOT_FOUND);
                                    assertThat(result.certificateHashDataChain()).isEmpty();

                                    var cmd = client.receivedCommands("GetInstalledCertificateIds").get(0);
                                    var types = cmd.payload().get("certificateType");
                                    assertThat(types.isArray()).isTrue();
                                    assertThat(types.get(0).asText()).isEqualTo("MORootCertificate");
                                    assertThat(types.get(1).asText()).isEqualTo("ManufacturerRootCertificate");
                                });
                                client.close();
                                return result;
                            });
                })
                .whenComplete((r, err) -> {
                    if (err != null) ctx.failNow(err);
                    else ctx.completeNow();
                });
    }
}
