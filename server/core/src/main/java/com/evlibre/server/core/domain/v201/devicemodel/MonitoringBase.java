package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code MonitoringBaseEnumType}: the station-wide monitoring set
 * a {@code SetMonitoringBase} request activates.
 *
 * <p>{@code All} enables every installed monitor (factory-configured plus any
 * added by the CSMS). {@code FactoryDefault} rolls back to the station's
 * factory monitor set. {@code HardWiredOnly} disables every software monitor
 * and leaves only those hardwired into the station firmware.
 */
public enum MonitoringBase {
    ALL,
    FACTORY_DEFAULT,
    HARD_WIRED_ONLY
}
