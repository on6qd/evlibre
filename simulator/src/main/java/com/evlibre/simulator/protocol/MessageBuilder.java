package com.evlibre.simulator.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

public class MessageBuilder {

    private final ObjectMapper om;
    private final boolean is201;

    public MessageBuilder(ObjectMapper objectMapper, String protocol) {
        this.om = objectMapper;
        this.is201 = "ocpp2.0.1".equals(protocol);
    }

    // --- BootNotification ---

    public JsonNode bootNotification(String vendor, String model) {
        if (is201) {
            ObjectNode station = om.createObjectNode()
                    .put("vendorName", vendor)
                    .put("model", model);
            ObjectNode node = om.createObjectNode().put("reason", "PowerUp");
            node.set("chargingStation", station);
            return node;
        } else {
            return om.createObjectNode()
                    .put("chargePointVendor", vendor)
                    .put("chargePointModel", model);
        }
    }

    // --- Heartbeat ---

    public JsonNode heartbeat() {
        return om.createObjectNode();
    }

    // --- StatusNotification ---

    public JsonNode statusNotification(int evseId, int connectorId, String status, Instant timestamp) {
        if (is201) {
            return om.createObjectNode()
                    .put("timestamp", timestamp.toString())
                    .put("connectorStatus", status)
                    .put("evseId", evseId)
                    .put("connectorId", connectorId);
        } else {
            return om.createObjectNode()
                    .put("connectorId", connectorId)
                    .put("status", status)
                    .put("errorCode", "NoError");
        }
    }

    // --- Authorize ---

    public JsonNode authorize(String idTag) {
        if (is201) {
            ObjectNode idToken = om.createObjectNode()
                    .put("idToken", idTag)
                    .put("type", "ISO14443");
            return om.createObjectNode().set("idToken", idToken);
        } else {
            return om.createObjectNode().put("idTag", idTag);
        }
    }

    // --- Start Transaction (v1.6) / TransactionEvent Started (v2.0.1) ---

    public JsonNode startTransaction(int connectorId, String idTag, long meterStart, Instant timestamp) {
        if (is201) {
            return transactionEvent("Started", "tx-" + System.nanoTime(), "Authorized",
                    0, connectorId, idTag, meterStart, timestamp);
        } else {
            return om.createObjectNode()
                    .put("connectorId", connectorId)
                    .put("idTag", idTag)
                    .put("meterStart", meterStart)
                    .put("timestamp", timestamp.toString());
        }
    }

    // --- Stop Transaction (v1.6) / TransactionEvent Ended (v2.0.1) ---

    public JsonNode stopTransaction(int transactionId, String txIdStr, String idTag,
                                     long meterStop, Instant timestamp, String reason) {
        if (is201) {
            ObjectNode txInfo = om.createObjectNode()
                    .put("transactionId", txIdStr)
                    .put("stoppedReason", reason);
            ObjectNode node = om.createObjectNode()
                    .put("eventType", "Ended")
                    .put("timestamp", timestamp.toString())
                    .put("triggerReason", "StopAuthorized")
                    .put("seqNo", 99);
            node.set("transactionInfo", txInfo);
            return node;
        } else {
            ObjectNode node = om.createObjectNode()
                    .put("transactionId", transactionId)
                    .put("meterStop", meterStop)
                    .put("timestamp", timestamp.toString());
            if (reason != null) {
                node.put("reason", reason);
            }
            return node;
        }
    }

    // --- MeterValues ---

    public JsonNode meterValues(int evseOrConnectorId, Integer transactionId, long meterWh, Instant timestamp) {
        ObjectNode sampledValue = om.createObjectNode()
                .put("value", String.valueOf(meterWh))
                .put("measurand", "Energy.Active.Import.Register");
        if (is201) {
            ObjectNode unit = om.createObjectNode().put("unit", "Wh");
            sampledValue.set("unitOfMeasure", unit);
        }

        ArrayNode sampledValues = om.createArrayNode().add(sampledValue);
        ObjectNode meterValue = om.createObjectNode()
                .put("timestamp", timestamp.toString());
        meterValue.set("sampledValue", sampledValues);
        ArrayNode meterValueArray = om.createArrayNode().add(meterValue);

        if (is201) {
            ObjectNode node = om.createObjectNode().put("evseId", evseOrConnectorId);
            node.set("meterValue", meterValueArray);
            return node;
        } else {
            ObjectNode node = om.createObjectNode().put("connectorId", evseOrConnectorId);
            if (transactionId != null) {
                node.put("transactionId", transactionId);
            }
            node.set("meterValue", meterValueArray);
            return node;
        }
    }

    // --- TransactionEvent (v2.0.1 only) ---

    public JsonNode transactionEvent(String eventType, String txId, String triggerReason,
                                      int seqNo, int connectorId, String idTag,
                                      long meterWh, Instant timestamp) {
        ObjectNode txInfo = om.createObjectNode().put("transactionId", txId);
        ObjectNode node = om.createObjectNode()
                .put("eventType", eventType)
                .put("timestamp", timestamp.toString())
                .put("triggerReason", triggerReason)
                .put("seqNo", seqNo);
        node.set("transactionInfo", txInfo);

        if (idTag != null) {
            ObjectNode idToken = om.createObjectNode()
                    .put("idToken", idTag)
                    .put("type", "ISO14443");
            node.set("idToken", idToken);
        }

        return node;
    }

    // --- Command responses ---

    public JsonNode remoteStartAccepted() {
        return om.createObjectNode().put("status", "Accepted");
    }

    public JsonNode remoteStopAccepted() {
        return om.createObjectNode().put("status", "Accepted");
    }

    public JsonNode resetAccepted() {
        return om.createObjectNode().put("status", "Accepted");
    }

    public JsonNode changeConfigurationAccepted() {
        return om.createObjectNode().put("status", "Accepted");
    }

    public JsonNode getConfiguration() {
        ObjectNode entry1 = om.createObjectNode()
                .put("key", "HeartbeatInterval").put("readonly", false).put("value", "60");
        ObjectNode entry2 = om.createObjectNode()
                .put("key", "MeterValueSampleInterval").put("readonly", false).put("value", "15");
        ObjectNode entry3 = om.createObjectNode()
                .put("key", "NumberOfConnectors").put("readonly", true).put("value", "1");
        ArrayNode configKeys = om.createArrayNode().add(entry1).add(entry2).add(entry3);
        return om.createObjectNode().set("configurationKey", configKeys);
    }

    public boolean is201() {
        return is201;
    }
}
