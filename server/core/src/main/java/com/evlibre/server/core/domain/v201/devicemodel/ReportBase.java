package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code ReportBaseEnumType} — which predefined base report the CSMS
 * asks the station to generate via {@code GetBaseReport} (B07).
 *
 * <p>Wire form uses PascalCase ({@code FullInventory}, ...); adapters map to/from.
 */
public enum ReportBase {
    /** Components and variables the operator is allowed to set. */
    CONFIGURATION_INVENTORY,
    /** Everything in the device model except monitoring settings. */
    FULL_INVENTORY,
    /** Availability- and problem-related components and variables only. */
    SUMMARY_INVENTORY
}
