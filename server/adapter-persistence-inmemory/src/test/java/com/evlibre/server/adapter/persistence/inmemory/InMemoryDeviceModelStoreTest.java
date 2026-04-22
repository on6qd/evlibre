package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.DataType;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.Mutability;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableAttribute;
import com.evlibre.server.core.domain.v201.devicemodel.VariableCharacteristics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDeviceModelStoreTest {

    private InMemoryDeviceModelStore store;
    private TenantId tenant;
    private ChargePointIdentity station;

    @BeforeEach
    void setUp() {
        store = new InMemoryDeviceModelStore();
        tenant = new TenantId("demo-tenant");
        station = new ChargePointIdentity("CHARGER-001");
    }

    @Test
    void findAll_returns_empty_when_no_reports_stored() {
        assertThat(store.findAll(tenant, station)).isEmpty();
    }

    @Test
    void upsert_stores_reports_and_findAll_returns_them() {
        var report = aReport("EVSE", "Available", "true", DataType.BOOLEAN);

        store.upsert(tenant, station, List.of(report));

        assertThat(store.findAll(tenant, station)).containsExactly(report);
    }

    @Test
    void upsert_with_same_variable_locator_overwrites_previous_row() {
        var initial = aReport("EVSE", "Available", "true", DataType.BOOLEAN);
        var updated = aReport("EVSE", "Available", "false", DataType.BOOLEAN);

        store.upsert(tenant, station, List.of(initial));
        store.upsert(tenant, station, List.of(updated));

        var rows = store.findAll(tenant, station);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).attributes().get(0).value()).isEqualTo("false");
    }

    @Test
    void upsert_with_different_variables_appends() {
        var available = aReport("EVSE", "Available", "true", DataType.BOOLEAN);
        var power = aReport("EVSE", "Power", "7400", DataType.DECIMAL);

        store.upsert(tenant, station, List.of(available));
        store.upsert(tenant, station, List.of(power));

        assertThat(store.findAll(tenant, station)).containsExactlyInAnyOrder(available, power);
    }

    @Test
    void upsert_isolates_stations_within_a_tenant() {
        var other = new ChargePointIdentity("CHARGER-002");
        var report = aReport("EVSE", "Available", "true", DataType.BOOLEAN);

        store.upsert(tenant, station, List.of(report));

        assertThat(store.findAll(tenant, other)).isEmpty();
    }

    @Test
    void upsert_isolates_tenants_for_the_same_station_identity() {
        var otherTenant = new TenantId("other-tenant");
        var report = aReport("EVSE", "Available", "true", DataType.BOOLEAN);

        store.upsert(tenant, station, List.of(report));

        assertThat(store.findAll(otherTenant, station)).isEmpty();
    }

    private ReportedVariable aReport(String componentName, String variableName, String value, DataType type) {
        return new ReportedVariable(
                Component.of(componentName, Evse.of(1)),
                Variable.of(variableName),
                List.of(new VariableAttribute(AttributeType.ACTUAL, value, Mutability.READ_WRITE, false, false)),
                VariableCharacteristics.of(type, true));
    }
}
