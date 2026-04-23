package com.evlibre.server.core.domain.v201.dto;

import java.util.Map;
import java.util.Objects;

public record DataTransferResult(
        DataTransferStatus status,
        Object data,
        String statusInfoReason,
        Map<String, Object> rawResponse) {

    public DataTransferResult {
        Objects.requireNonNull(status, "status");
        rawResponse = rawResponse == null ? Map.of() : Map.copyOf(rawResponse);
    }

    public static DataTransferResult of(DataTransferStatus status) {
        return new DataTransferResult(status, null, null, Map.of());
    }

    public boolean isAccepted() {
        return status == DataTransferStatus.ACCEPTED;
    }
}
