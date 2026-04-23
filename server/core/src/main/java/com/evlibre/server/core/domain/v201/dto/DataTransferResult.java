package com.evlibre.server.core.domain.v201.dto;

import java.util.Objects;

public record DataTransferResult(
        DataTransferStatus status,
        Object data,
        String statusInfoReason) {

    public DataTransferResult {
        Objects.requireNonNull(status, "status");
    }

    public static DataTransferResult of(DataTransferStatus status) {
        return new DataTransferResult(status, null, null);
    }

    public boolean isAccepted() {
        return status == DataTransferStatus.ACCEPTED;
    }
}
