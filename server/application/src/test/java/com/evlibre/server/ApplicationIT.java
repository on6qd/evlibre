package com.evlibre.server;

import com.evlibre.server.config.ServerConfig;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v16.model.AuthorizationStatus;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@link Application#start(ServerConfig)}. Boots the full CSMS on
 * ephemeral ports with in-memory persistence, verifies both servers bound a port,
 * verifies the demo-tenant seed ran, then closes cleanly. This is the contract the
 * planned SAP-simulator acceptance IT (TESTPLAN.md §5) depends on.
 */
@Tag("integration")
class ApplicationIT {

    @Test
    void start_binds_ports_seeds_demo_data_and_closes_cleanly() throws Exception {
        ServerConfig config = loadTestConfig();

        try (AppHandle handle = Application.start(config)) {
            assertThat(handle.ocppPort())
                    .as("OCPP server should have bound an ephemeral port")
                    .isGreaterThan(0);
            assertThat(handle.webUiPort())
                    .as("Web UI server should have bound an ephemeral port")
                    .isGreaterThan(0);
            assertThat(handle.ocppPort()).isNotEqualTo(handle.webUiPort());

            assertThat(handle.tenantRepository().findByTenantId(new TenantId("demo-tenant")))
                    .as("demo-tenant should be seeded in in-memory mode")
                    .isPresent();

            assertThat(handle.authorizationRepository()
                    .findStatusByIdTag(new TenantId("demo-tenant"), "TAG001"))
                    .as("TAG001 should be seeded and accepted")
                    .contains(AuthorizationStatus.ACCEPTED);
        }
    }

    private static ServerConfig loadTestConfig() throws Exception {
        URL url = ApplicationIT.class.getClassLoader().getResource("server-test.toml");
        assertThat(url).as("server-test.toml must be on the test classpath").isNotNull();
        return new TomlMapper().readValue(url, ServerConfig.class);
    }
}
