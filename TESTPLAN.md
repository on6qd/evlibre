# EVLibre Test Plan

## Automated Test Coverage

Most of this plan is now automated. Run:

- **Unit tests only** (fast, ~2s): `mvn test`
- **Unit + integration tests**: `mvn verify`

| Layer | Tests | Command |
|-------|-------|---------|
| Domain models & value objects | 21 unit tests | `mvn test -pl common,server/core` |
| Use cases (all 8) | 22 unit tests | `mvn test -pl server/core` |
| OCPP codec & schema | 16 unit tests | `mvn test -pl server/adapter-ocpp-ws` |
| Persistence adapters | 12 unit tests | `mvn test -pl server/adapter-persistence-inmemory,server/adapter-persistence-h2` |
| OCPP handlers (all v1.6 + v2.0.1) | 18 integration tests | `mvn verify -pl server/adapter-ocpp-ws` |
| Web UI (dashboard, stations, errors) | 6 integration tests | `mvn verify -pl server/adapter-ui-web` |
| **Total** | **81 unit + 24 integration** | `mvn verify` |

---

## Manual Tests (remaining)

The following tests require manual verification and are not automated:

## Prerequisites

1. Build the project:
   ```bash
   mvn clean package
   ```
2. Start the application:
   ```bash
   java -jar server/application/target/application-1.0-SNAPSHOT.jar
   ```
3. Verify console shows both servers starting:
   - OCPP WebSocket on port **9090**
   - Web UI on port **8080**
4. Tool: use `websocat` (or any WebSocket client) for OCPP tests, and a browser/curl for Web UI tests.

> **Tip:** For H2 persistence tests, edit `server.toml` and set `database.type = "h2-file"`, then restart.

---

## 1. Application Startup

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 1.1 | Default startup (in-memory) | Start with default `server.toml` | Logs show OCPP WS on 9090, Web UI on 8080, in-memory persistence |
| 1.2 | H2 persistence startup | Set `database.type = "h2-file"` in `server.toml`, start | Logs show Flyway migrations running, H2 file created in `./data/` |
| 1.3 | Custom config path | `java -jar ... --config /tmp/custom.toml` | App uses custom config values |
| 1.4 | Missing config file | Delete/rename `server.toml`, start | App starts with sensible defaults |

---

## 2. Web UI

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 2.1 | Landing page | `GET http://localhost:8080/` | Landing page renders |
| 2.2 | Dashboard | `GET http://localhost:8080/demo-tenant/dashboard` | Dashboard with stats (0 stations initially for in-memory) |
| 2.3 | Station list (empty) | `GET http://localhost:8080/demo-tenant/stations` | Empty station list page |
| 2.4 | Station list (after registration) | Register a station via OCPP (test 3.1), then reload stations page | Station appears in list |
| 2.5 | Unknown tenant | `GET http://localhost:8080/unknown-tenant/dashboard` | Error page / 404 |

---

## 3. OCPP 1.6 — BootNotification

Connect WebSocket to: `ws://localhost:9090/ocpp/demo-tenant/CHARGER-001` with sub-protocol `ocpp1.6`

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 3.1 | Successful registration | Send: `[2,"msg001","BootNotification",{"chargePointVendor":"TestVendor","chargePointModel":"TestModel"}]` | Response: `[3,"msg001",{"status":"Accepted","currentTime":"...","interval":900}]` |
| 3.2 | Re-registration | Send BootNotification again on same connection | Response: Accepted, station updated |
| 3.3 | Unknown tenant | Connect to `ws://localhost:9090/ocpp/unknown-tenant/CHARGER-001` | Connection rejected or BootNotification returns Rejected |
| 3.4 | Invalid payload | Send: `[2,"msg002","BootNotification",{"invalid":"data"}]` | Error response: `[4,"msg002","FormationViolation",...]` |

---

## 4. OCPP 1.6 — Heartbeat

Prerequisite: Station registered (test 3.1).

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 4.1 | Heartbeat | Send: `[2,"msg010","Heartbeat",{}]` | Response: `[3,"msg010",{"currentTime":"..."}]` |
| 4.2 | Verify timestamp update | Send heartbeat, check station list in UI | `lastHeartbeat` timestamp updated |

---

## 5. OCPP 1.6 — StatusNotification

Prerequisite: Station registered.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 5.1 | Connector status | Send: `[2,"msg020","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]` | Response: `[3,"msg020",{}]` |
| 5.2 | Charging status | Send: `[2,"msg021","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Charging"}]` | Response: `[3,"msg021",{}]` |

---

## 6. OCPP 1.6 — Authorize

Prerequisite: Station registered.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 6.1 | Valid tag | Send: `[2,"msg030","Authorize",{"idTag":"TAG001"}]` | Response: `[3,"msg030",{"idTagInfo":{"status":"Accepted"}}]` |
| 6.2 | Unknown tag | Send: `[2,"msg031","Authorize",{"idTag":"UNKNOWN"}]` | Response: `[3,"msg031",{"idTagInfo":{"status":"Invalid"}}]` |

---

## 7. OCPP 1.6 — Start Transaction

Prerequisite: Station registered.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 7.1 | Start with valid tag | Send: `[2,"msg040","StartTransaction",{"connectorId":1,"idTag":"TAG001","meterStart":0,"timestamp":"2026-04-11T10:00:00Z"}]` | Response: `[3,"msg040",{"transactionId":<id>,"idTagInfo":{"status":"Accepted"}}]` — note the `transactionId` |
| 7.2 | Start with invalid tag | Send: `[2,"msg041","StartTransaction",{"connectorId":1,"idTag":"BADTAG","meterStart":0,"timestamp":"2026-04-11T10:00:00Z"}]` | Response contains `"status":"Invalid"` |

---

## 8. OCPP 1.6 — MeterValues

Prerequisite: Active transaction (test 7.1).

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 8.1 | Send meter values | Send: `[2,"msg050","MeterValues",{"connectorId":1,"transactionId":<id>,"meterValue":[{"timestamp":"2026-04-11T10:15:00Z","sampledValue":[{"value":"1500"}]}]}]` | Response: `[3,"msg050",{}]` |

---

## 9. OCPP 1.6 — Stop Transaction

Prerequisite: Active transaction from test 7.1.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 9.1 | Stop transaction | Send: `[2,"msg060","StopTransaction",{"transactionId":<id>,"meterStop":5000,"timestamp":"2026-04-11T11:00:00Z","reason":"EVDisconnected"}]` | Response: `[3,"msg060",{"idTagInfo":{"status":"Accepted"}}]` |
| 9.2 | Stop non-existent transaction | Send StopTransaction with `transactionId: 99999` | Error or rejection |

---

## 10. OCPP 1.6 — Error Handling

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 10.1 | Unknown action | Send: `[2,"msg070","FakeAction",{}]` | Response: `[4,"msg070","NotImplemented","...",{}]` |
| 10.2 | Malformed JSON | Send: `not json at all` | Connection stays open, error response or ignored |
| 10.3 | Invalid message type | Send: `[9,"msg071","Heartbeat",{}]` | Error response |

---

## 11. OCPP 2.0.1 — BootNotification

Connect WebSocket to: `ws://localhost:9090/ocpp/demo-tenant/CHARGER-201` with sub-protocol `ocpp2.0.1`

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 11.1 | Successful registration | Send: `[2,"msg100","BootNotification",{"reason":"PowerUp","chargingStation":{"model":"Model201","vendorName":"TestVendor"}}]` | Response: `[3,"msg100",{"status":"Accepted","currentTime":"...","interval":900}]` |
| 11.2 | Invalid payload | Send: `[2,"msg101","BootNotification",{"bad":"data"}]` | Error response: `[4,"msg101","FormationViolation",...]` |

---

## 12. OCPP 2.0.1 — Heartbeat

Prerequisite: Station registered via 2.0.1.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 12.1 | Heartbeat | Send: `[2,"msg110","Heartbeat",{}]` | Response: `[3,"msg110",{"currentTime":"..."}]` |

---

## 13. OCPP 2.0.1 — StatusNotification

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 13.1 | Status update | Send: `[2,"msg120","StatusNotification",{"timestamp":"2026-04-11T10:00:00Z","connectorStatus":"Available","evseId":1,"connectorId":1}]` | Response: `[3,"msg120",{}]` |

---

## 14. OCPP 2.0.1 — Authorize

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 14.1 | Valid tag | Send: `[2,"msg130","Authorize",{"idToken":{"idToken":"TAG001","type":"ISO14443"}}]` | Response with `"status":"Accepted"` |
| 14.2 | Unknown tag | Send: `[2,"msg131","Authorize",{"idToken":{"idToken":"UNKNOWN","type":"ISO14443"}}]` | Response with `"status":"Invalid"` |

---

## 15. OCPP 2.0.1 — TransactionEvent

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 15.1 | Transaction started | Send: `[2,"msg140","TransactionEvent",{"eventType":"Started","timestamp":"2026-04-11T10:00:00Z","triggerReason":"Authorized","seqNo":0,"transactionInfo":{"transactionId":"tx-001"}}]` | Response: `[3,"msg140",{}]` (or with idTokenInfo) |
| 15.2 | Transaction updated | Send: `[2,"msg141","TransactionEvent",{"eventType":"Updated","timestamp":"2026-04-11T10:30:00Z","triggerReason":"MeterValuePeriodic","seqNo":1,"transactionInfo":{"transactionId":"tx-001"}}]` | Response: `[3,"msg141",{}]` |
| 15.3 | Transaction ended | Send: `[2,"msg142","TransactionEvent",{"eventType":"Ended","timestamp":"2026-04-11T11:00:00Z","triggerReason":"EVCommunicationLost","seqNo":2,"transactionInfo":{"transactionId":"tx-001","stoppedReason":"EVDisconnected"}}]` | Response: `[3,"msg142",{}]` |

---

## 16. OCPP 2.0.1 — MeterValues

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 16.1 | Send meter values | Send: `[2,"msg150","MeterValues",{"evseId":1,"meterValue":[{"timestamp":"2026-04-11T10:15:00Z","sampledValue":[{"value":1500.0}]}]}]` | Response: `[3,"msg150",{}]` |

---

## 17. Protocol Negotiation

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 17.1 | Request ocpp1.6 | Connect with sub-protocol `ocpp1.6` | Server accepts, negotiates 1.6 |
| 17.2 | Request ocpp2.0.1 | Connect with sub-protocol `ocpp2.0.1` | Server accepts, negotiates 2.0.1 |
| 17.3 | Request both | Connect with sub-protocols `ocpp1.6, ocpp2.0.1` | Server picks one (likely 2.0.1) |
| 17.4 | Unsupported protocol | Connect with sub-protocol `ocpp1.5` | Connection rejected |
| 17.5 | No sub-protocol | Connect without specifying sub-protocol | Connection rejected or default behavior |

---

## 18. Multi-Tenancy

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 18.1 | Tenant isolation | Register stations on `demo-tenant`, verify they don't appear under another tenant's UI | Stations scoped to tenant |
| 18.2 | Same station ID, different tenants | Register `CHARGER-001` on two different tenants (requires second tenant in DB) | Both coexist independently |

---

## 19. Persistence (H2 mode)

Set `database.type = "h2-file"` and restart.

| # | Test | Steps | Expected |
|---|------|-------|----------|
| 19.1 | Data survives restart | Register station, stop app, restart | Station still present in UI |
| 19.2 | Demo seed data | Start fresh (delete `./data/`), restart | `demo-tenant` exists, `TAG001`/`TAG002` are seeded |
| 19.3 | Flyway migrations | Check logs on startup | All V1–V6 migrations applied |

---

## 20. Full Charging Session (End-to-End)

This is the happy-path scenario simulating a complete charging session over OCPP 1.6.

1. Connect `ws://localhost:9090/ocpp/demo-tenant/CHARGER-E2E` (sub-protocol: `ocpp1.6`)
2. **BootNotification** → Accepted
3. **StatusNotification** (connector 1, Available)
4. **Authorize** (TAG001) → Accepted
5. **StartTransaction** (connector 1, TAG001, meterStart=0) → get transactionId
6. **StatusNotification** (connector 1, Charging)
7. **MeterValues** (1500 Wh)
8. **MeterValues** (3500 Wh)
9. **StopTransaction** (meterStop=5000, reason=EVDisconnected)
10. **StatusNotification** (connector 1, Available)
11. **Heartbeat** → verify currentTime
12. **Verify in Web UI**: station visible, dashboard stats updated

**Expected**: All steps succeed, station and transaction visible in the UI, data consistent.
