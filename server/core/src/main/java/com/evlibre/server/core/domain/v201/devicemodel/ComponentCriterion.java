package com.evlibre.server.core.domain.v201.devicemodel;

/**
 * OCPP 2.0.1 {@code ComponentCriterionEnumType}: predicate used by the CSMS to
 * filter which components a {@code GetReport} (B08) should include. At most
 * four criteria may be combined in a single request.
 *
 * <p>Wire form uses PascalCase ({@code Active}, {@code Available}, ...);
 * adapters map to/from these constants.
 */
public enum ComponentCriterion {
    /** Components whose {@code Active} variable is true. */
    ACTIVE,
    /** Components whose {@code Available} variable is true. */
    AVAILABLE,
    /** Components whose {@code Enabled} variable is true. */
    ENABLED,
    /** Components whose {@code Problem} variable is true. */
    PROBLEM
}
