# EVLibre Test Strategy

This document is the authoritative view of how evlibre is tested: what runs automatically, what is still manual, and where the gaps are. It is organised as a test pyramid — unit at the base, in-process integration in the middle, SAP-simulator acceptance at the top.

Last refreshed at commit `bdf1858` (Phases 0–8 complete — OCPP 2.0.1 feature parity done; see `OCPP201_TODO.md`).

---

## 1. Pyramid at a glance

| Layer | What it covers | Command | Where tests live | Roughly |
|---|---|---|---|---|
| **Unit** | Domain model invariants, use-case happy/sad paths, codec + schema validator, persistence adapters against their contracts | `mvn test` | `*Test.java` under each module's `src/test/java` | ~608 tests, ~10 s |
| **In-process integration** | Full WebSocket stack + dispatcher + handlers + use cases, exercised against an in-memory test harness (`OcppTestHarness`); plus `ApplicationIT` smoke test for the `start(ServerConfig)` test hook | `mvn verify` | `*IT.java` tagged `@Tag("integration")` in `adapter-ocpp-ws`, `adapter-ui-web`, and `application` | ~175 tests, ~30 s |
| **SAP simulator acceptance** | The real SAP OCPP client driving the CSMS end-to-end over a live WebSocket | Manual today (`docker compose up` in `reference-simulators/`). **Automation is planned — see §5.** | `reference-simulators/` | N/A |
| **Manual / exploratory** | Startup config permutations, browser smoke tests, multi-tenant spot checks, H2 persistence across restarts | Human | §3 of this doc | ~15 min |

Totals as of the refresh commit: **783 tests passing** (`mvn verify`). Counts per module in §2.

---

## 2. Automated coverage — what runs today

### 2.1 Unit tests (`mvn test`)

The surefire/failsafe split is done by JUnit 5 `@Tag`: surefire excludes `@Tag("integration")`, failsafe includes it. Both plugins are configured once in the root `pom.xml`; modules inherit.

| Module | Tests | What's covered |
|---|---|---|
| `common` | 19 | `ChargePointIdentity`, `ConnectorId`, `EvseId`, `OcppProtocol` parsing, `MessageTypeId` |
| `server/core` | 523 | All v1.6 use cases under `usecases/v16/*UseCaseTest`; the full v2.0.1 surface under `usecases/v201/*UseCaseTest` — every outbound command from Phases 1–8 and every inbound buffering use case has its own direct test; plus domain records (`ChargingProfile`, `Firmware`, `CertificateHashDataChain`, `MessageInfo`, …) that enforce spec cross-field rules at construction |
| `server/adapter-ocpp-ws` (surefire) | 48 | `OcppMessageCodecTest`, `OcppSchemaValidatorTest`, `OcppWebSocketIntegrationTest`, `OcppStationCommandSenderTest` (the **protocol-enforcement guard** — see §2.3), plus `handler/v201/*Test` for the three inbound handlers with non-trivial parsing (`DataTransferHandler201Test`, `NotifyReportHandler201Test`, `ReportChargingProfilesHandler201Test`) |
| `server/adapter-persistence-inmemory` | 14 | Repository contract tests, including `InMemoryDeviceModelRepositoryTest` |
| `server/adapter-persistence-h2` | 4 | Same contracts against H2 + Flyway |
| `server/adapter-ui-web` (surefire) | 0 | All Web UI tests are integration-tagged |

### 2.2 In-process integration tests (`mvn verify`)

All ITs extend `VertxExtension` and drive the full OCPP stack via `OcppTestHarness` (in `server/adapter-ocpp-ws/src/test/java/.../testutil/`). The harness wires both v1.6 and v2.0.1 handlers from their respective use case sets, with fakes from `server/test-support/`.

**`server/adapter-ocpp-ws` failsafe — 168 tests across 30 IT classes**, grouped below:

*Cross-protocol and framing:*

| IT class | Purpose |
|---|---|
| `OcppAllHandlersIT` | Broad coverage of every wired v1.6 + v2.0.1 inbound handler; exit criteria per action |
| `OcppChargingSessionIT` | End-to-end happy-path charging session for both v1.6 and v2.0.1 |
| `Ocpp201HandlersIT` | v2.0.1-only suite; includes **cross-protocol negative tests** (a v1.6 action over a v2.0.1 session must return `NotImplemented`, and vice-versa) |
| `SubProtocolNegotiationIT` | WebSocket subprotocol handshake (accepts `ocpp1.6` / `ocpp2.0.1`, rejects `ocpp1.5`) |

*v1.6 outbound profiles (CSMS → CS commands):*

| IT class | Purpose |
|---|---|
| `CoreProfileCommandIT` | Reset, ChangeAvailability, UnlockConnector, ClearCache, RemoteStart/Stop |
| `FirmwareProfileCommandIT` | GetDiagnostics, UpdateFirmware |
| `LocalAuthListProfileIT` | GetLocalListVersion, SendLocalList |
| `ReservationProfileIT` | ReserveNow, CancelReservation |
| `SmartChargingProfileIT` | SetChargingProfile, ClearChargingProfile, GetCompositeSchedule |
| `TriggerMessageProfileIT` | TriggerMessage for v1.6 actions |

*v2.0.1 outbound commands (Phases 1–8):*

| IT class | Purpose |
|---|---|
| `ProvisioningCommand201IT` | GetBaseReport, GetReport, GetVariables, SetVariables, SetNetworkProfile, Reset |
| `RemoteControlCommand201IT` | RequestStartTransaction, RequestStopTransaction, TriggerMessage, UnlockConnector |
| `AvailabilityAndAuthCommand201IT` | ChangeAvailability, ClearCache, GetLocalListVersion, SendLocalList |
| `ReservationCommand201IT` | ReserveNow, CancelReservation |
| `TransactionCommand201IT` | GetTransactionStatus |
| `SmartChargingCommand201IT` | SetChargingProfile, ClearChargingProfile, GetCompositeSchedule, GetChargingProfiles |
| `FirmwareCommand201IT` | UpdateFirmware, PublishFirmware |
| `DiagnosticsCommand201IT` | GetLog, SetVariableMonitoring, ClearVariableMonitoring, SetMonitoringBase, SetMonitoringLevel, GetMonitoringReport, CustomerInformation, display-message commands, CostUpdated |
| `SecurityCommand201IT` | GetInstalledCertificateIds, InstallCertificate, DeleteCertificate |

*v2.0.1 inbound flows (Phases 1–8):*

| IT class | Purpose |
|---|---|
| `DataTransfer201IT` | DataTransfer inbound + outbound (Block P) |
| `DeviceModelReporting201IT` | GetBaseReport → multi-frame NotifyReport → repo populated + completion event fires once |
| `ReportChargingProfiles201IT` | Station-initiated ReportChargingProfiles (K09) |
| `ChargingLimit201IT` | NotifyChargingLimit + ClearedChargingLimit |
| `EVCharging201IT` | NotifyEVChargingNeeds + NotifyEVChargingSchedule (ISO 15118) |
| `Firmware201IT` | FirmwareStatusNotification + PublishFirmwareStatusNotification |
| `Diagnostics201IT` | LogStatusNotification + NotifyEvent |
| `Security201IT` | SecurityEventNotification, SignCertificate, GetCertificateStatus, Get15118EVCertificate |
| `MonitoringReporting201IT` | GetMonitoringReport → multi-frame NotifyMonitoringReport → repo-upsert + completion event |
| `CustomerInformation201IT` | CustomerInformation(report=true) → 3-frame NotifyCustomerInformation → single sink call |
| `DisplayMessages201IT` | GetDisplayMessages → multi-frame NotifyDisplayMessages aggregation |

**`server/adapter-ui-web` failsafe — 6 tests:**

| IT class | Purpose |
|---|---|
| `WebUiIT` | Landing page, dashboard, station list, error pages, tenant isolation over HTTP |

### 2.3 The OCPP 2.0.1 separation contract

The Phase 0 refactor added a strict rule: v1.6 and v2.0.1 must not share domain types, use cases, ports, or handlers. Three layers of guard tests enforce it:

- **Compile-time** — the package layout itself (`domain.shared.*` / `domain.v16.*` / `domain.v201.*`, `usecases.v16.*` / `usecases.v201.*`). A v1.6 use case accidentally referencing a v2.0.1 type would require an import from the wrong package and stand out in review.
- **`OcppStationCommandSenderTest`** (5 cases) — unit-tests the outbound port split. Proves that `.v16()` rejects calls against a v2.0.1-negotiated session, `.v201()` rejects calls against a v1.6-negotiated session, and that each succeeds against its own protocol.
- **`Ocpp201HandlersIT`** — two negative tests assert the dispatcher's `protocol:action` keying: a v1.6 action sent over a v2.0.1 session and vice-versa both return `NotImplemented` on the wire.

Any PR that blurs the separation should fail at least one of these three checks.

### 2.4 Schema validation wiring

`OcppSchemaValidator` is plumbed through four checkpoints and every one is a hard reject on failure (Phase 0.4c):

- Inbound CALL (station → CSMS requests) — validates against `schemas/{protocol}/<Action>[Request].json`. Rejects with an OCPP CALLERROR.
- Inbound CALL_RESULT (station's response to our outbound CALL) — validates against `<Action>Response.json` in `OcppPendingCallManager`. Rejects (fails the pending-call future).
- Outbound CALL (commands we send to the station) — validates in `OcppStationCommandSender` before writing to the wire. Rejects (fails the outbound future, nothing hits the wire).
- Outbound CALL_RESULT (our response to a station's request) — validates in `OcppWebSocketVerticle` before sending. Rejects.

The schema library covers every wired action in both protocols: all 28 v1.6 actions (Core + Smart Charging + Local Auth + Reservation + Remote Trigger + Firmware + Diagnostics) and all 10 v2.0.1 actions with request schemas have matching response schemas on classpath under `resources/schemas/ocpp16/` and `resources/schemas/ocpp201/`. The validator's contract remains "missing schema = valid", so any new action landing without a schema is not accidentally blocked — it simply skips validation until schemas are authored.

---

## 3. Manual / exploratory

These scenarios are cheap to run in minutes and cover behaviours not worth automating at this stage.

### 3.1 Startup

```bash
mvn -q clean package -DskipTests
java -jar server/application/target/application-1.0-SNAPSHOT.jar
```

Verify the console shows OCPP WebSocket on **9090** and Web UI on **9080**. Variants worth spot-checking when touching config loading:

| # | Test | Expected |
|---|---|---|
| 3.1.1 | Default `server.toml` | In-memory persistence, both ports bound |
| 3.1.2 | `database.type = "h2-file"` in `server.toml` | Flyway logs V1–V7 migrations; `./data/` created |
| 3.1.3 | `java -jar … --config /tmp/custom.toml` | App uses the custom path |
| 3.1.4 | Delete/rename `server.toml` | App starts with defaults |

### 3.2 Web UI smoke (browser)

With a station registered via OCPP (or the SAP simulator running), visit:

- `http://localhost:9080/` — landing
- `http://localhost:9080/demo-tenant/dashboard` — stats reflect connected stations
- `http://localhost:9080/demo-tenant/stations` — registered stations list and detail pages
- `http://localhost:9080/unknown-tenant/dashboard` — tenant-scoped error page

Automated HTTP-level coverage exists in `WebUiIT`; this step is just visual confirmation of rendered HTML.

### 3.3 Multi-tenant spot checks

| # | Test | Expected |
|---|---|---|
| 3.3.1 | Register stations on `demo-tenant`; ensure they don't appear under a second tenant | Stations scoped per tenant |
| 3.3.2 | Register the same station ID (`CHARGER-001`) on two tenants | Both coexist independently |

### 3.4 H2 persistence across restart

1. Set `database.type = "h2-file"`, start, register a station over OCPP.
2. Stop and restart.
3. Station should still appear in the Web UI; `demo-tenant` + `TAG001` / `TAG002` seeded from migrations V6–V7.

*(The per-OCPP-action websocat walkthrough that used to live here was dropped. `OcppAllHandlersIT` covers it end-to-end; redoing it by hand no longer adds signal.)*

---

## 4. SAP reference simulator — acceptance layer (manual today)

`reference-simulators/` runs the upstream [SAP e-mobility-charging-stations-simulator](https://github.com/SAP/e-mobility-charging-stations-simulator) against a locally-running CSMS. This is the closest we get to a real-client test today.

### 4.1 Running it

1. Start the CSMS locally on port 9090 (§3.1).
2. `cd reference-simulators && docker compose up --build`.
3. Watch the CSMS logs for connections from the SAP stations and for the expected OCPP flow.

See `reference-simulators/README.md` for the canonical run instructions.

### 4.2 What the simulator does

- **Supervision URL**: `ws://host.docker.internal:9090/ocpp/demo-tenant` (hardcoded in `sap-simulator/config.json`; the test IT described in §5 will override this).
- **Station templates**: `sap-simulator/station-16.json` (v1.6, baseName `SAP-16`) and `sap-simulator/station-201.json` (v2.0.1, baseName `CS-KEBA-201`). Heartbeat every 60 s, meter values every 15 s (v1.6) / 30 s (v2.0.1).
- **Auto-transactions**: `probabilityOfStart: 1`, duration 60–300 s, stop after ~18 min, then re-start. Container runs indefinitely.
- **Id tags**: `sap-simulator/idtags.json` = `["TAG001", "TAG002"]` — matches the CSMS's migration-seeded tags.
- **Station count** (current): `config.json` configures **10 × v1.6** (`SAP-16-0`..`SAP-16-9`). The 2.0.1 template exists on disk but is **not** currently referenced by `stationTemplateUrls`. The README mentions "2x 1.6, 2x 2.0.1" — this is stale. Reconcile before implementing §5.
- **No query API**: the SAP sim is log-only. Acceptance tests must assert on CSMS-side state, not on sim output.

### 4.3 Planned automation — see §5.

---

## 5. Planned: SAP-sim acceptance IT (design; not yet implemented)

**Goal.** Turn the manual SAP-sim run into a tagged, opt-in automated test so a regression in protocol handling can be caught before a release without a human watching logs.

**Non-goals.** Replace the in-process ITs. Run on every push. Assert on sim-side logs.

### 5.1 Module layout

Create `server/acceptance-sap-it/`:

```
server/acceptance-sap-it/
├── pom.xml                                    (adds testcontainers + application + test-support test deps)
└── src/test/java/com/evlibre/acceptance/
    └── SapSimulatorAcceptanceIT.java          (tagged @Tag("sap-acceptance"))
```

A Maven profile `-Psap` activates failsafe with `<groups>sap-acceptance</groups>`. Default `mvn verify` ignores the tag, so dev machines without Docker are unaffected.

### 5.2 Container lifecycle

- Testcontainers `ImageFromDockerfile` pointed at `reference-simulators/Dockerfile` — reuses the existing build. No separate published image.
- Bind-mount a **test-scoped** `config.json` that points at the CSMS on `host.testcontainers.internal:<port>` (the port is picked by `Testcontainers.exposeHostPorts(…)`). The production `sap-simulator/config.json` is unchanged.
- Station count for the IT: small (e.g. 2 × v1.6 + 2 × v2.0.1) to keep runtime under ~3 minutes. Set via a copied station template.

### 5.3 CSMS lifecycle

- `Application.start(ServerConfig)` returns an `AppHandle` (see §6.3). Tests construct a `ServerConfig` with `websocket_port = 0` / `webui.port = 0`, call `start`, and read `handle.ocppPort()` for the container-side config.
- Test boots the CSMS on port 0, captures the bound port, passes it to the container via the mounted config.
- Uses the in-memory persistence adapter (no DB setup). Seeds `demo-tenant` and `TAG001` / `TAG002`.

### 5.4 Assertions (on CSMS state)

Use AssertJ `await()` with a generous timeout (~2 min) — then assert:

- `stationRepo.count() == expectedStationCount` — the container's stations all connected and boot-registered.
- `eventLog` contains, for **each** station: `BootNotification`, `Heartbeat`, `Authorize`. v1.6 stations additionally show `StartTransaction` + `MeterValues`; v2.0.1 stations show `TransactionEvent` + `MeterValues`.
- `transactionRepo` has at least one non-empty transaction per v1.6 station.
- For v2.0.1 stations, confirm the v201 handlers ran by checking event-log entries were attributed to the v2.0.1 protocol (the `OcppEventLogPort` records protocol).

Assertions stop at *proof of life for each use case*, not specific values — timing varies between runs.

### 5.5 CI considerations

- Add a GitHub Actions workflow at `.github/workflows/ci.yml` (not in repo today) with two jobs: `unit` (`mvn verify`) on every push, `acceptance-sap` (`mvn verify -Psap`) nightly or on `main` only.
- The SAP build inside the container clones the upstream repo at `--depth 1`; pin to a known-good upstream commit in `Dockerfile` so a breaking upstream change can't silently fail a release.

---

## 6. Gaps / deferred

### 6.1 Performance / soak
No load test, no soak test, no benchmark. The SAP-sim IT (§5) with larger station counts and a longer duration can double as a light soak harness, but isn't one today.

### 6.2 Chaos — dropped WebSocket, reconnect, stale pending calls
Not covered at any layer. `OcppPendingCallManager` has a 30 s timeout but the code path is not exercised by any test. Add to future work.

### 6.3 `Application` test-hook — done 2026-04-24
`Application.start(ServerConfig)` returns an `AppHandle` (implements `AutoCloseable`) exposing `Vertx`, the actual bound OCPP / Web UI ports, and the repositories acceptance tests assert on (tenant, station, transaction, authorization, event log). `main` delegates to it. `ApplicationIT` smoke-tests the contract: boots on ephemeral ports, verifies seeding, closes cleanly. Startup failures now throw instead of being silently logged.

### Closed
- **Response + v1.6 parity schemas** (Phase 0.4, 2026-04-21) — every wired action in both protocols now has request + response schemas on classpath; the warn-only validation checkpoints in §2.4 are hard rejections.
- **v2.0.1 DTO reshape** (Phase 0.1 closure, 2026-04-24) — `StatusNotificationData201` and `MeterValuesData201` live under `domain/v201/dto/`; all v201 inbound handlers and use cases bind to the v201-shaped DTOs. No v201 path references a v16 DTO.

---

## Running everything

```bash
# Fast feedback (unit only, ~10 s)
mvn test

# Full automated suite (unit + in-process IT, ~40 s)
mvn verify

# Full + SAP acceptance (requires Docker; not yet implemented — see §5)
mvn verify -Psap
```
