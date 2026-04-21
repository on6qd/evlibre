package com.evlibre.server.adapter.ocpp.testutil;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory for OCPP WebSocket messages used in integration tests.
 * Each method returns a complete OCPP CALL frame as a JSON string.
 */
public final class OcppMessages {

    private OcppMessages() {}

    private static String nextId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // --- OCPP 1.6 ---

    public static String bootNotification16(String vendor, String model) {
        return bootNotification16(nextId(), vendor, model);
    }

    public static String bootNotification16(String msgId, String vendor, String model) {
        return String.format("[2,\"%s\",\"BootNotification\","
                + "{\"chargePointVendor\":\"%s\",\"chargePointModel\":\"%s\"}]", msgId, vendor, model);
    }

    public static String heartbeat16() {
        return heartbeat16(nextId());
    }

    public static String heartbeat16(String msgId) {
        return String.format("[2,\"%s\",\"Heartbeat\",{}]", msgId);
    }

    public static String authorize16(String idTag) {
        return authorize16(nextId(), idTag);
    }

    public static String authorize16(String msgId, String idTag) {
        return String.format("[2,\"%s\",\"Authorize\",{\"idTag\":\"%s\"}]", msgId, idTag);
    }

    public static String statusNotification16(int connectorId, String status, String errorCode) {
        return statusNotification16(nextId(), connectorId, status, errorCode);
    }

    public static String statusNotification16(String msgId, int connectorId, String status, String errorCode) {
        return String.format("[2,\"%s\",\"StatusNotification\","
                + "{\"connectorId\":%d,\"status\":\"%s\",\"errorCode\":\"%s\"}]",
                msgId, connectorId, status, errorCode);
    }

    public static String startTransaction16(int connectorId, String idTag, long meterStart, Instant timestamp) {
        return startTransaction16(nextId(), connectorId, idTag, meterStart, timestamp);
    }

    public static String startTransaction16(String msgId, int connectorId, String idTag, long meterStart, Instant timestamp) {
        return String.format("[2,\"%s\",\"StartTransaction\","
                + "{\"connectorId\":%d,\"idTag\":\"%s\",\"meterStart\":%d,\"timestamp\":\"%s\"}]",
                msgId, connectorId, idTag, meterStart, timestamp);
    }

    public static String stopTransaction16(int transactionId, long meterStop, Instant timestamp, String reason) {
        return stopTransaction16(nextId(), transactionId, meterStop, timestamp, reason);
    }

    public static String stopTransaction16(String msgId, int transactionId, long meterStop, Instant timestamp, String reason) {
        return String.format("[2,\"%s\",\"StopTransaction\","
                + "{\"transactionId\":%d,\"meterStop\":%d,\"timestamp\":\"%s\",\"reason\":\"%s\"}]",
                msgId, transactionId, meterStop, timestamp, reason);
    }

    public static String meterValues16(int connectorId, Integer transactionId, String value, Instant timestamp) {
        return meterValues16(nextId(), connectorId, transactionId, value, timestamp);
    }

    public static String meterValues16(String msgId, int connectorId, Integer transactionId, String value, Instant timestamp) {
        String txPart = transactionId != null ? String.format(",\"transactionId\":%d", transactionId) : "";
        return String.format("[2,\"%s\",\"MeterValues\","
                + "{\"connectorId\":%d%s,\"meterValue\":[{\"timestamp\":\"%s\",\"sampledValue\":[{\"value\":\"%s\"}]}]}]",
                msgId, connectorId, txPart, timestamp, value);
    }

    // --- OCPP 2.0.1 ---

    public static String bootNotification201(String vendorName, String model) {
        return bootNotification201(nextId(), vendorName, model);
    }

    public static String bootNotification201(String msgId, String vendorName, String model) {
        return String.format("[2,\"%s\",\"BootNotification\","
                + "{\"chargingStation\":{\"vendorName\":\"%s\",\"model\":\"%s\"},\"reason\":\"PowerUp\"}]",
                msgId, vendorName, model);
    }

    public static String heartbeat201() {
        return heartbeat201(nextId());
    }

    public static String heartbeat201(String msgId) {
        return String.format("[2,\"%s\",\"Heartbeat\",{}]", msgId);
    }

    public static String authorize201(String idToken, String type) {
        return authorize201(nextId(), idToken, type);
    }

    public static String authorize201(String msgId, String idToken, String type) {
        return String.format("[2,\"%s\",\"Authorize\","
                + "{\"idToken\":{\"idToken\":\"%s\",\"type\":\"%s\"}}]", msgId, idToken, type);
    }

    public static String statusNotification201(int evseId, int connectorId, String status, Instant timestamp) {
        return statusNotification201(nextId(), evseId, connectorId, status, timestamp);
    }

    public static String statusNotification201(String msgId, int evseId, int connectorId, String status, Instant timestamp) {
        return String.format("[2,\"%s\",\"StatusNotification\","
                + "{\"timestamp\":\"%s\",\"connectorStatus\":\"%s\",\"evseId\":%d,\"connectorId\":%d}]",
                msgId, timestamp, status, evseId, connectorId);
    }

    public static String transactionEvent201(String eventType, String txId, String triggerReason, int seqNo, Instant timestamp) {
        return transactionEvent201(nextId(), eventType, txId, triggerReason, seqNo, timestamp);
    }

    public static String transactionEvent201(String msgId, String eventType, String txId, String triggerReason, int seqNo, Instant timestamp) {
        return String.format("[2,\"%s\",\"TransactionEvent\","
                + "{\"eventType\":\"%s\",\"timestamp\":\"%s\",\"triggerReason\":\"%s\","
                + "\"seqNo\":%d,\"transactionInfo\":{\"transactionId\":\"%s\"}}]",
                msgId, eventType, timestamp, triggerReason, seqNo, txId);
    }

    public static String meterValues201(int evseId, String value, Instant timestamp) {
        return meterValues201(nextId(), evseId, value, timestamp);
    }

    public static String meterValues201(String msgId, int evseId, String value, Instant timestamp) {
        return String.format("[2,\"%s\",\"MeterValues\","
                + "{\"evseId\":%d,\"meterValue\":[{\"timestamp\":\"%s\",\"sampledValue\":[{\"value\":%s}]}]}]",
                msgId, evseId, timestamp, value);
    }

    public static String notifyReport201(int requestId, int seqNo, Instant generatedAt,
                                          String componentName, String variableName) {
        return notifyReport201(nextId(), requestId, seqNo, generatedAt, componentName, variableName);
    }

    public static String notifyReport201(String msgId, int requestId, int seqNo, Instant generatedAt,
                                          String componentName, String variableName) {
        return String.format("[2,\"%s\",\"NotifyReport\","
                + "{\"requestId\":%d,\"generatedAt\":\"%s\",\"seqNo\":%d,"
                + "\"reportData\":[{\"component\":{\"name\":\"%s\"},"
                + "\"variable\":{\"name\":\"%s\"},"
                + "\"variableCharacteristics\":{\"dataType\":\"string\",\"supportsMonitoring\":false},"
                + "\"variableAttribute\":[{\"type\":\"Actual\",\"value\":\"42\"}]}]}]",
                msgId, requestId, generatedAt, seqNo, componentName, variableName);
    }

    // --- OCPP 1.6 additional CS→CSMS messages ---

    public static String dataTransfer16(String vendorId, String messageId, String data) {
        return dataTransfer16(nextId(), vendorId, messageId, data);
    }

    public static String dataTransfer16(String msgId, String vendorId, String messageId, String data) {
        String messagePart = messageId != null ? String.format(",\"messageId\":\"%s\"", messageId) : "";
        String dataPart = data != null ? String.format(",\"data\":\"%s\"", data) : "";
        return String.format("[2,\"%s\",\"DataTransfer\",{\"vendorId\":\"%s\"%s%s}]",
                msgId, vendorId, messagePart, dataPart);
    }

    public static String diagnosticsStatusNotification16(String status) {
        return diagnosticsStatusNotification16(nextId(), status);
    }

    public static String diagnosticsStatusNotification16(String msgId, String status) {
        return String.format("[2,\"%s\",\"DiagnosticsStatusNotification\",{\"status\":\"%s\"}]", msgId, status);
    }

    public static String firmwareStatusNotification16(String status) {
        return firmwareStatusNotification16(nextId(), status);
    }

    public static String firmwareStatusNotification16(String msgId, String status) {
        return String.format("[2,\"%s\",\"FirmwareStatusNotification\",{\"status\":\"%s\"}]", msgId, status);
    }

    // --- Error cases ---

    public static String unknownAction(String action) {
        return String.format("[2,\"%s\",\"%s\",{}]", nextId(), action);
    }

    public static String malformedCall(String msgId) {
        return String.format("[2,\"%s\",\"BootNotification\",{\"chargePointVendor\":\"ABB\"}]", msgId);
    }
}
