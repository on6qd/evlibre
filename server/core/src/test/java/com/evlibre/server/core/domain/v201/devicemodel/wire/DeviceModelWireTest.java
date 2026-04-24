package com.evlibre.server.core.domain.v201.devicemodel.wire;

import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Evse;
import com.evlibre.server.core.domain.v201.devicemodel.MonitorType;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableMonitor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceModelWireTest {

    @Test
    void attribute_type_round_trip_for_every_value() {
        for (AttributeType t : AttributeType.values()) {
            String wire = DeviceModelWire.attributeTypeToWire(t);
            assertThat(DeviceModelWire.attributeTypeFromWire(wire)).isEqualTo(t);
        }
    }

    @Test
    void attribute_type_wire_uses_spec_pascal_case() {
        assertThat(DeviceModelWire.attributeTypeToWire(AttributeType.ACTUAL)).isEqualTo("Actual");
        assertThat(DeviceModelWire.attributeTypeToWire(AttributeType.TARGET)).isEqualTo("Target");
        assertThat(DeviceModelWire.attributeTypeToWire(AttributeType.MIN_SET)).isEqualTo("MinSet");
        assertThat(DeviceModelWire.attributeTypeToWire(AttributeType.MAX_SET)).isEqualTo("MaxSet");
    }

    @Test
    void unknown_attribute_type_wire_raises() {
        assertThatThrownBy(() -> DeviceModelWire.attributeTypeFromWire("Nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AttributeType")
                .hasMessageContaining("Nope");
    }

    @Test
    void component_minimal_round_trips() {
        Component original = Component.of("SecurityCtrlr");
        Map<String, Object> wire = DeviceModelWire.componentToWire(original);

        assertThat(wire).containsEntry("name", "SecurityCtrlr")
                .doesNotContainKey("instance")
                .doesNotContainKey("evse");
        assertThat(DeviceModelWire.componentFromWire(wire)).isEqualTo(original);
    }

    @Test
    void component_with_instance_and_evse_round_trips() {
        Component original = new Component("EVSE", "primary", Evse.of(1, 2));
        Map<String, Object> wire = DeviceModelWire.componentToWire(original);

        assertThat(wire).containsEntry("name", "EVSE").containsEntry("instance", "primary");
        assertThat(wire.get("evse")).isInstanceOf(Map.class);
        assertThat(DeviceModelWire.componentFromWire(wire)).isEqualTo(original);
    }

    @Test
    void variable_round_trips_with_and_without_instance() {
        assertThat(DeviceModelWire.variableFromWire(
                DeviceModelWire.variableToWire(Variable.of("BasicAuthPassword"))))
                .isEqualTo(Variable.of("BasicAuthPassword"));

        Variable withInstance = new Variable("AvailabilityState", "a");
        assertThat(DeviceModelWire.variableFromWire(
                DeviceModelWire.variableToWire(withInstance))).isEqualTo(withInstance);
    }

    @Test
    void variable_omits_null_instance_on_wire() {
        Map<String, Object> wire = DeviceModelWire.variableToWire(Variable.of("V"));
        assertThat(wire).containsOnlyKeys("name");
    }

    @Test
    void evse_round_trips_with_and_without_connector() {
        Evse whole = Evse.of(1);
        assertThat(DeviceModelWire.evseFromWire(DeviceModelWire.evseToWire(whole)))
                .isEqualTo(whole);

        Evse specific = Evse.of(2, 3);
        assertThat(DeviceModelWire.evseFromWire(DeviceModelWire.evseToWire(specific)))
                .isEqualTo(specific);
    }

    @Test
    void evse_accepts_numeric_ids_from_deserialisers_returning_long() {
        // Jackson may hand back Long/Integer depending on payload size — the decoder
        // must normalize via Number.intValue().
        Map<String, Object> wire = Map.of("id", 5L, "connectorId", 7L);
        assertThat(DeviceModelWire.evseFromWire(wire)).isEqualTo(Evse.of(5, 7));
    }

    @Test
    void monitor_type_round_trip_for_every_value() {
        for (MonitorType t : MonitorType.values()) {
            String wire = DeviceModelWire.monitorTypeToWire(t);
            assertThat(DeviceModelWire.monitorTypeFromWire(wire)).isEqualTo(t);
        }
    }

    @Test
    void monitor_type_wire_uses_spec_pascal_case() {
        assertThat(DeviceModelWire.monitorTypeToWire(MonitorType.UPPER_THRESHOLD)).isEqualTo("UpperThreshold");
        assertThat(DeviceModelWire.monitorTypeToWire(MonitorType.LOWER_THRESHOLD)).isEqualTo("LowerThreshold");
        assertThat(DeviceModelWire.monitorTypeToWire(MonitorType.DELTA)).isEqualTo("Delta");
        assertThat(DeviceModelWire.monitorTypeToWire(MonitorType.PERIODIC)).isEqualTo("Periodic");
        assertThat(DeviceModelWire.monitorTypeToWire(MonitorType.PERIODIC_CLOCK_ALIGNED)).isEqualTo("PeriodicClockAligned");
    }

    @Test
    void unknown_monitor_type_wire_raises() {
        assertThatThrownBy(() -> DeviceModelWire.monitorTypeFromWire("Nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MonitorType")
                .hasMessageContaining("Nope");
    }

    @Test
    void variable_monitor_round_trips() {
        VariableMonitor original = new VariableMonitor(42, true, 15.5, MonitorType.UPPER_THRESHOLD, 3);
        Map<String, Object> wire = DeviceModelWire.variableMonitorToWire(original);

        assertThat(wire)
                .containsEntry("id", 42)
                .containsEntry("transaction", true)
                .containsEntry("value", 15.5)
                .containsEntry("type", "UpperThreshold")
                .containsEntry("severity", 3);

        assertThat(DeviceModelWire.variableMonitorFromWire(wire)).isEqualTo(original);
    }

    @Test
    void variable_monitor_accepts_numeric_ids_from_deserialisers_returning_long() {
        Map<String, Object> wire = Map.of(
                "id", 5L,
                "transaction", false,
                "value", 30,
                "type", "Periodic",
                "severity", 5L);

        VariableMonitor m = DeviceModelWire.variableMonitorFromWire(wire);

        assertThat(m).isEqualTo(new VariableMonitor(5, false, 30.0, MonitorType.PERIODIC, 5));
    }
}
