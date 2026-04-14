package com.evlibre.server.core.domain.dto;

import java.util.Map;

/**
 * Result of a CSMS-to-CS command. Wraps the status string from the charge station's response.
 */
public record CommandResult(String status, Map<String, Object> rawResponse) {

    public CommandResult(String status) {
        this(status, Map.of());
    }

    public boolean isAccepted() {
        return "Accepted".equals(status);
    }
}
