package com.evlibre.server.adapter.ocpp.handler.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.common.ocpp.OcppProtocol;
import com.evlibre.server.adapter.ocpp.OcppSession;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleReportChargingProfilesPort;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingLimitSource;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfile;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfileKind;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingProfilePurpose;
import com.evlibre.server.core.domain.v201.smartcharging.ChargingRateUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportChargingProfilesHandler201Test {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TenantId tenantId = new TenantId("demo-tenant");
    private final ChargePointIdentity stationIdentity = new ChargePointIdentity("CS-201");

    private CapturingPort port;
    private ReportChargingProfilesHandler201 handler;
    private OcppSession session;

    @BeforeEach
    void setUp() {
        port = new CapturingPort();
        handler = new ReportChargingProfilesHandler201(port, objectMapper);
        session = new OcppSession(tenantId, stationIdentity, OcppProtocol.OCPP_201, null);
    }

    @Test
    void parses_single_profile_and_delegates_to_port() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 77,
                  "chargingLimitSource": "CSO",
                  "evseId": 2,
                  "tbc": false,
                  "chargingProfile": [{
                    "id": 9,
                    "stackLevel": 1,
                    "chargingProfilePurpose": "TxDefaultProfile",
                    "chargingProfileKind": "Relative",
                    "chargingSchedule": [{
                      "id": 1,
                      "chargingRateUnit": "A",
                      "chargingSchedulePeriod": [
                        {"startPeriod": 0, "limit": 16.0}
                      ]
                    }]
                  }]
                }""");

        JsonNode response = handler.handle(session, "msg-1", payload);

        assertThat(response.isObject()).isTrue();
        assertThat(response.size()).isZero();
        assertThat(port.frames).hasSize(1);

        CapturingPort.Frame f = port.frames.get(0);
        assertThat(f.tenantId).isEqualTo(tenantId);
        assertThat(f.stationIdentity).isEqualTo(stationIdentity);
        assertThat(f.requestId).isEqualTo(77);
        assertThat(f.source).isEqualTo(ChargingLimitSource.CSO);
        assertThat(f.evseId).isEqualTo(2);
        assertThat(f.tbc).isFalse();
        assertThat(f.profiles).hasSize(1);

        ChargingProfile p = f.profiles.get(0);
        assertThat(p.id()).isEqualTo(9);
        assertThat(p.stackLevel()).isEqualTo(1);
        assertThat(p.chargingProfilePurpose()).isEqualTo(ChargingProfilePurpose.TX_DEFAULT_PROFILE);
        assertThat(p.chargingProfileKind()).isEqualTo(ChargingProfileKind.RELATIVE);
        assertThat(p.chargingSchedule()).hasSize(1);
        assertThat(p.chargingSchedule().get(0).chargingRateUnit()).isEqualTo(ChargingRateUnit.AMPERES);
        assertThat(p.chargingSchedule().get(0).chargingSchedulePeriod()).hasSize(1);
    }

    @Test
    void parses_tx_profile_with_transaction_id_and_recurring_kind() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 1,
                  "chargingLimitSource": "EMS",
                  "evseId": 3,
                  "chargingProfile": [{
                    "id": 100,
                    "stackLevel": 5,
                    "chargingProfilePurpose": "TxProfile",
                    "chargingProfileKind": "Absolute",
                    "transactionId": "tx-abc",
                    "validFrom": "2027-01-01T00:00:00Z",
                    "chargingSchedule": [{
                      "id": 1,
                      "startSchedule": "2027-01-01T00:00:00Z",
                      "chargingRateUnit": "W",
                      "chargingSchedulePeriod": [
                        {"startPeriod": 0, "limit": 22000.0, "numberPhases": 3}
                      ]
                    }]
                  }]
                }""");

        handler.handle(session, "msg-2", payload);

        CapturingPort.Frame f = port.frames.get(0);
        assertThat(f.source).isEqualTo(ChargingLimitSource.EMS);
        ChargingProfile p = f.profiles.get(0);
        assertThat(p.chargingProfilePurpose()).isEqualTo(ChargingProfilePurpose.TX_PROFILE);
        assertThat(p.transactionId()).isEqualTo("tx-abc");
        assertThat(p.validFrom()).isNotNull();
        assertThat(p.chargingSchedule().get(0).chargingSchedulePeriod().get(0).numberPhases()).isEqualTo(3);
    }

    @Test
    void missing_tbc_defaults_to_false() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "requestId": 7,
                  "chargingLimitSource": "SO",
                  "evseId": 0,
                  "chargingProfile": [{
                    "id": 1,
                    "stackLevel": 0,
                    "chargingProfilePurpose": "ChargingStationMaxProfile",
                    "chargingProfileKind": "Absolute",
                    "chargingSchedule": [{
                      "id": 1,
                      "startSchedule": "2027-03-01T00:00:00Z",
                      "chargingRateUnit": "W",
                      "chargingSchedulePeriod": [{"startPeriod": 0, "limit": 50000.0}]
                    }]
                  }]
                }""");

        handler.handle(session, "msg-3", payload);

        assertThat(port.frames.get(0).tbc).isFalse();
        assertThat(port.frames.get(0).evseId).isEqualTo(0);
        assertThat(port.frames.get(0).source).isEqualTo(ChargingLimitSource.SO);
    }

    private static final class CapturingPort implements HandleReportChargingProfilesPort {

        record Frame(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId,
                     ChargingLimitSource source, int evseId, List<ChargingProfile> profiles, boolean tbc) {}

        final List<Frame> frames = new ArrayList<>();

        @Override
        public void handleFrame(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId,
                                ChargingLimitSource source, int evseId,
                                List<ChargingProfile> profiles, boolean tbc) {
            frames.add(new Frame(tenantId, stationIdentity, requestId, source, evseId, profiles, tbc));
        }
    }
}
