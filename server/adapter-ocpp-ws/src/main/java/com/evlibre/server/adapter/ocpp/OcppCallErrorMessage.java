package com.evlibre.server.adapter.ocpp;

import com.evlibre.common.ocpp.OcppErrorCode;

public record OcppCallErrorMessage(String messageId, OcppErrorCode errorCode,
                                    String errorDescription, String errorDetails) {}
