package com.evlibre.server.core.usecases.v201;

import com.evlibre.server.core.domain.v201.model.AdditionalInfo;
import com.evlibre.server.core.domain.v201.model.AuthorizationStatus;
import com.evlibre.server.core.domain.v201.model.IdToken;
import com.evlibre.server.core.domain.v201.model.IdTokenInfo;
import com.evlibre.server.core.domain.v201.model.IdTokenType;
import com.evlibre.server.core.domain.v201.model.MessageContent;
import com.evlibre.server.core.domain.v201.model.MessageFormat;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire-form codec for OCPP 2.0.1 authorization-domain types shared across
 * outbound use cases (RequestStartTransaction, SendLocalList, ReserveNow,
 * CustomerInformation, ...). Keeps the wire shape in one place so the field
 * names and enum spellings stay consistent across every CALL we emit.
 *
 * <p>Package-private: callers are the v201 use cases only.
 */
final class IdTokenWire {

    private IdTokenWire() {}

    static Map<String, Object> toWire(IdToken token) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idToken", token.idToken());
        out.put("type", typeToWire(token.type()));
        if (token.additionalInfo() != null) {
            List<Map<String, Object>> extras = new ArrayList<>(token.additionalInfo().size());
            for (AdditionalInfo info : token.additionalInfo()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("additionalIdToken", info.additionalIdToken());
                entry.put("type", info.type());
                extras.add(entry);
            }
            out.put("additionalInfo", extras);
        }
        return out;
    }

    static Map<String, Object> idTokenInfoToWire(IdTokenInfo info) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", authStatusToWire(info.status()));
        if (info.cacheExpiryDateTime() != null) {
            out.put("cacheExpiryDateTime", DateTimeFormatter.ISO_INSTANT.format(info.cacheExpiryDateTime()));
        }
        if (info.chargingPriority() != null) {
            out.put("chargingPriority", info.chargingPriority());
        }
        if (info.evseId() != null) {
            out.put("evseId", List.copyOf(info.evseId()));
        }
        if (info.groupIdToken() != null) {
            out.put("groupIdToken", toWire(info.groupIdToken()));
        }
        if (info.language1() != null) {
            out.put("language1", info.language1());
        }
        if (info.language2() != null) {
            out.put("language2", info.language2());
        }
        if (info.personalMessage() != null) {
            out.put("personalMessage", messageContentToWire(info.personalMessage()));
        }
        return out;
    }

    static String typeToWire(IdTokenType t) {
        return switch (t) {
            case CENTRAL -> "Central";
            case EMAID -> "eMAID";
            case ISO14443 -> "ISO14443";
            case ISO15693 -> "ISO15693";
            case KEY_CODE -> "KeyCode";
            case LOCAL -> "Local";
            case MAC_ADDRESS -> "MacAddress";
            case NO_AUTHORIZATION -> "NoAuthorization";
        };
    }

    static String authStatusToWire(AuthorizationStatus s) {
        return switch (s) {
            case ACCEPTED -> "Accepted";
            case BLOCKED -> "Blocked";
            case CONCURRENT_TX -> "ConcurrentTx";
            case EXPIRED -> "Expired";
            case INVALID -> "Invalid";
            case NO_CREDIT -> "NoCredit";
            case NOT_ALLOWED_TYPE_EVSE -> "NotAllowedTypeEVSE";
            case NOT_AT_THIS_LOCATION -> "NotAtThisLocation";
            case NOT_AT_THIS_TIME -> "NotAtThisTime";
            case UNKNOWN -> "Unknown";
        };
    }

    private static Map<String, Object> messageContentToWire(MessageContent m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("format", messageFormatToWire(m.format()));
        if (m.language() != null) {
            out.put("language", m.language());
        }
        out.put("content", m.content());
        return out;
    }

    private static String messageFormatToWire(MessageFormat f) {
        return switch (f) {
            case ASCII -> "ASCII";
            case HTML -> "HTML";
            case URI -> "URI";
            case UTF8 -> "UTF8";
        };
    }
}
