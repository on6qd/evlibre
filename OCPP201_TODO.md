# OCPP 2.0.1 Implementation TODO

Source: OCPP 2.0.1 Edition 4 Errata February 2026 (NotebookLM).
Last audit: 2026-04-21.
Phase 0 completed: 2026-04-21 (all subsections done — separation groundwork
plus full response-schema authoring and hard-reject validation, see 0.4).

## Architectural rule: strict 1.6 / 2.0.1 separation

- **Shared**: `ChargingStation` identity, `Tenant`, negotiated protocol, protocol-neutral enums that match both specs (`RegistrationStatus`, `ConnectorStatus`, `TransactionStatus`), `TimeProvider`.
- **Split per protocol** (no reuse across versions): domain models, DTOs, repositories/ports, use cases, inbound handlers, outbound command senders, JSON schemas.
- **No protocol branching inside a use case.** No 1.6 → 2.0.1 translation. If the business concept differs in shape between versions, it gets two classes.
- **Ports constrained by protocol**: an `Ocpp201StationCommandSender` rejects the call if the session's negotiated protocol is 1.6, and vice-versa.

---

## Current state (audit 2026-04-21)

### Already in place for v2.0.1
- Inbound wire layer: `OcppMessageCodec` (version-agnostic), `OcppMessageDispatcher` (keys on `protocol:action`), `OcppSession.protocol()`, `OcppProtocolNegotiator`, `OcppSchemaValidator` (version-keyed).
- v2.0.1 inbound handlers exist under `adapter-ocpp-ws/.../handler/v201/`:
  `BootNotificationHandler201`, `HeartbeatHandler201`, `StatusNotificationHandler201`,
  `AuthorizeHandler201`, `TransactionEventHandler201`, `MeterValuesHandler201`,
  `NotifyReportHandler201` *(exists but NOT registered in dispatcher — dead code)*.
- v2.0.1 request schemas on classpath (`resources/schemas/ocpp201/`):
  `BootNotificationRequest`, `HeartbeatRequest`, `AuthorizeRequest`,
  `StatusNotificationRequest`, `MeterValuesRequest`, `TransactionEventRequest`,
  `RequestStartTransactionRequest`, `RequestStopTransactionRequest`, `ResetRequest`.
- v2.0.1-shaped domain/DTO classes already present (but mixed in with v1.6 under the same packages):
  `DeviceModelVariable`, `DeviceModelPort`, `TransactionEventData`, `TransactionEventResult`,
  `HandleTransactionEventUseCase`.

### Violations of the separation rule — must be fixed before new v2.0.1 work
- v201 inbound handlers call **shared v1.6-shaped use cases**:
  `RegisterStationUseCase`, `HandleHeartbeatUseCase`, `AuthorizeUseCase`,
  `HandleStatusNotificationUseCase`, `HandleMeterValuesUseCase` are invoked from both v16 and v201 handlers today.
- v1.6-shaped domain/DTO types are being used by v201 paths:
  `AuthorizationResult`, `AuthorizationStatus` enum, `StatusNotificationData`, `MeterValuesData`, `IdTagInfo`.
- All 20 outbound use cases are v1.6-only and call a **generic** `StationCommandSender` port that never consults `OcppSession.protocol()`.
- No v201 outbound use cases exist.

### Validation gaps (affect both protocols, but blocker for v2.0.1)
- Inbound `CALL_RESULT` we send back to the station: **not validated**.
- Outbound `CALL` we send to the station: **not validated**.
- `CALL_RESULT` we receive from the station (response to our outbound command): **not validated**.
- No response schemas on classpath for either protocol.

### v2.0.1-shaped entities that need to be introduced
- `IdToken` (object: idToken + type + additionalInfo + optional groupIdToken) — replaces `idTag` string.
- `Evse` as a first-class entity between `ChargingStation` and `Connector`.
- 2.0.1 `Transaction` (UUID id, state machine via `TransactionEvent`, `evseId`, `IdToken`).
- 2.0.1 authorization status enum (different values than 1.6).
- Device Model: `Component`, `Variable`, `VariableAttribute`, `VariableCharacteristics`, `Monitor`.
- Display messages, variable monitoring configuration.
- ISO 15118 certificate / charging-needs entities.

---

## Phase 0 — Separation groundwork (prerequisite for everything else) ✅ DONE

### 0.1 — Split core domain packages ✅
- [x] Create `server/core/.../domain/shared/`, `.../domain/v16/`, `.../domain/v201/`.
- [x] Move to `shared`: `ChargingStation`, `Tenant`, `TenantId`, `ConnectorStatus`, `RegistrationStatus`, `TransactionStatus`, `TimeProvider`, `StationRegistration`, `RegistrationResult`, `CommandResult`, `StationRepositoryPort`, `TenantRepositoryPort`, `StationEventPublisher`, `OcppEventLogPort`. (`ChargePointIdentity` + `ConnectorId` were already in the `common` module, protocol-neutral.)
- [x] Move to `v16`: `Transaction`, `IdTagInfo`, `Reservation`, `ReservationStatus`, `StationConfigurationKey`, `StartTransactionData`, `StartTransactionResult`, `StopTransactionData`, `AuthorizationResult`, `AuthorizationStatus`, `TransactionRepositoryPort`, `ReservationRepositoryPort`, `AuthorizationRepositoryPort`, `StationConfigurationPort`, `StatusNotificationData`, `MeterValuesData`.
- [x] Move to `v201`: `DeviceModelVariable`, `DeviceModelPort`, `TransactionEventData`, `TransactionEventResult`.
- [ ] **Deferred to phase 1+**: reshape `StatusNotificationData` / `MeterValuesData` per protocol (currently both in `v16`; `v201` versions will be introduced when EVSE-shaped data is needed).

### 0.2 — Split use cases ✅
- [x] Move existing use cases to `usecases/v16/` (all 29).
- [x] Create `usecases/v201/` siblings: `RegisterStationUseCaseV201`, `HandleHeartbeatUseCaseV201`, `AuthorizeUseCaseV201`, `HandleStatusNotificationUseCaseV201`, `HandleMeterValuesUseCaseV201`. (Near-identical clones of the v16 versions; they'll diverge as v2.0.1-specific shapes land.)
- [x] Rebind v201 inbound handlers to v201 use cases; v16 handlers continue to use v16 use cases.
- [x] `HandleTransactionEventUseCase` lives in `usecases/v201/`.

### 0.3 — Outbound port split ✅
- [x] `StationCommandSender` → `Ocpp16StationCommandSender` in `domain.v16.ports.outbound`.
- [x] New `Ocpp201StationCommandSender` in `domain.v201.ports.outbound`.
- [x] `OcppStationCommandSender` exposes `.v16()` / `.v201()` accessors; each enforces the session's negotiated protocol and rejects mismatched calls.
- [x] All v1.6 outbound use cases now depend on `Ocpp16StationCommandSender` only.

### 0.4 — Schema + validation hardening ✅
- [x] Register `NotifyReportHandler201` in the dispatcher + test harness.
- [x] Add `NotifyReportRequest` schema.
- [x] Extend `OcppSchemaValidator` with `validateRequest` / `validateResponse`; wire into outbound CALL (request schema), outbound CALL_RESULT (response schema), and station-sent CALL_RESULT (response schema via the pending-call manager).
- [x] Author v2.0.1 response schemas for all 10 actions with request schemas on classpath (`BootNotification`, `Heartbeat`, `Authorize`, `StatusNotification`, `MeterValues`, `TransactionEvent`, `NotifyReport`, `RequestStartTransaction`, `RequestStopTransaction`, `Reset`).
- [x] Author v1.6 response schemas for all 28 actions with request schemas on classpath (Core, Smart Charging, Local Auth, Reservation, Remote Trigger, Firmware, Diagnostics).
- [x] Flip warn-only checks in `OcppStationCommandSender` (outbound CALL), `OcppWebSocketVerticle` (our CALL_RESULT self-check), and `OcppPendingCallManager` (station CALL_RESULT) to hard rejections — missing-schema contract remains "valid" so non-wired actions aren't blocked.
- [x] Fixed pre-existing spec-violation while flipping: v1.6 DataTransfer use case now returns `UnknownVendorId` (spec value) rather than `UnknownVendor`.

### 0.5 — Test harness ✅
- [x] `OcppTestHarness` wires v16 and v201 handlers from their respective use case sets (done as part of 0.2b).
- [x] `Ocpp201HandlersIT` added as a v2.0.1-only integration suite, with cross-protocol isolation tests that prove a v1.6 action over a v2.0.1 session (and vice-versa) is rejected at the dispatcher.

---

## Phase 1 — Core Provisioning (Block B)
Device Model is the biggest 2.0.1-only concept; foundational for everything else.
- [~] Device Model domain in `domain/v201/devicemodel/`: `Component`, `Evse`, `Variable`, `VariableAttribute`, `VariableCharacteristics` + `DeviceModelRepositoryPort` + `DeviceModelWire` codec landed across phases 1.1–1.6 and the 1.11 refactor; plus `ReportedVariable` aggregate wired through `NotifyReportHandler201`. **`Monitor` deferred to Phase 8** where variable monitoring (`SetMonitoringBase`, `NotifyMonitoringReport`, ...) first depends on it.
- [x] Outbound (new v201 use case + handler + schemas): `GetBaseReport` — `GetBaseReportUseCaseV201` + schemas; `PostBootActionService` now delegates to the use case.
- [x] Outbound (new v201 use case + handler + schemas): `GetReport` — `GetReportUseCaseV201` + schemas; supports `ComponentCriterion` and `ComponentVariableSelector` filters.
- [x] Outbound (new v201 use case + handler + schemas): `GetVariables` — `GetVariablesUseCaseV201` + schemas; synchronous read, returns typed `List<GetVariableResult>`.
- [x] Outbound (new v201 use case + handler + schemas): `SetVariables` — `SetVariablesUseCaseV201` + schemas; synchronous write, returns `List<SetVariableResult>` including the spec's `RebootRequired` outcome.
- [x] Outbound (new v201 use case + handler + schemas): `SetNetworkProfile` — `SetNetworkProfileUseCaseV201` + schemas; full `NetworkConnectionProfile` with optional APN + VPN subtypes.
- [x] Outbound (new v201 use case + handler + schemas): `Reset` — `ResetStationUseCaseV201` (independent of v1.6); supports `Immediate`/`OnIdle` + optional `evseId`, and the added `Scheduled` response status.

### Phase 1 follow-ups (not blockers for starting Phase 2)
- [x] `NotifyReport` request-scoped aggregation. `HandleNotifyReportUseCaseV201` buffers frames per `(tenantId, stationIdentity, requestId)` and on `tbc=false` upserts atomically and fires `NotifyReportCompletionPort`.
- [x] End-to-end IT: `DeviceModelReporting201IT` drives `GetBaseReport` → N `NotifyReport` frames with matching `requestId` → repo populated, completion event fires exactly once.

## Phase 2 — Remote Control (Block F)
All v2.0.1-only use cases; no reuse of v1.6 siblings.
- [ ] Outbound: `RequestStartTransaction` — `RequestStartTransactionUseCase` under `usecases/v201/`, builds `{remoteStartId, idToken:{idToken,type}, evseId?, groupIdToken?, chargingProfile?}`.
- [ ] Outbound: `RequestStopTransaction` — `RequestStopTransactionUseCase` under `usecases/v201/`, builds `{transactionId}` (UUID).
- [ ] Outbound: `TriggerMessage` (v2.0.1 enum: `BootNotification`, `LogStatusNotification`, `FirmwareStatusNotification`, `Heartbeat`, `MeterValues`, `SignChargingStationCertificate`, `SignV2GCertificate`, `StatusNotification`, `TransactionEvent`, `SignCombinedCertificate`, `PublishFirmwareStatusNotification`).
- [ ] Outbound: `UnlockConnector` (v2.0.1 uses `evseId` + `connectorId`).

## Phase 3 — Availability, Auth, Local List (Blocks C, D, G)
- [ ] Outbound: `ChangeAvailability` — uses EVSE/connector `OperationalStatus`.
- [ ] Outbound: `ClearCache`.
- [ ] Outbound: `GetLocalListVersion`.
- [ ] Outbound: `SendLocalList` — v2.0.1 `AuthorizationData` carries `IdToken` object + `IdTokenInfo`.

## Phase 4 — Generic Extension Point (Block P)
- [ ] Inbound `DataTransferHandler201` + use case + schemas.
- [ ] Outbound `DataTransferUseCaseV201` + schemas.

## Phase 5 — Reservations, Transactions, Smart Charging (Blocks E, H, K)
- [ ] Outbound: `ReserveNow`, `CancelReservation` (v2.0.1 uses `IdToken`, `evseId`, `connectorType`).
- [ ] Outbound: `GetTransactionStatus` (2.0.1-only).
- [ ] Outbound: `SetChargingProfile`, `ClearChargingProfile`, `GetCompositeSchedule`.
- [ ] Outbound: `GetChargingProfiles` (2.0.1-only).
- [ ] Inbound: `ReportChargingProfiles`, `NotifyChargingLimit`, `ClearedChargingLimit`.
- [ ] Inbound: `NotifyEVChargingNeeds`, `NotifyEVChargingSchedule` (ISO 15118).

## Phase 6 — Firmware & Diagnostics (Blocks L, N)
- [ ] Inbound: `FirmwareStatusNotification`.
- [ ] Outbound: `UpdateFirmware`.
- [ ] Inbound: `LogStatusNotification`.
- [ ] Outbound: `GetLog` (replaces 1.6 `GetDiagnostics` conceptually, but different shape).
- [ ] Inbound: `NotifyEvent`.
- [ ] Inbound/Outbound: `PublishFirmware` + `PublishFirmwareStatusNotification` (optional, local firmware distribution).

## Phase 7 — Security & Certificates (Blocks A, M)
- [ ] Inbound: `SecurityEventNotification`.
- [ ] Inbound: `SignCertificate`.
- [ ] Outbound: `GetInstalledCertificateIds`, `InstallCertificate`, `DeleteCertificate`.
- [ ] Outbound: `GetCertificateStatus`.
- [ ] Inbound: `Get15118EVCertificate` (ISO 15118 Plug & Charge).

## Phase 8 — Monitoring & Display (Blocks N, O, I)
Largely 2.0.1-only features.
- [ ] Variable monitoring: `SetMonitoringBase`, `SetMonitoringLevel`, `SetVariableMonitoring`, `ClearVariableMonitoring`, `GetMonitoringReport`.
- [ ] Inbound: `NotifyMonitoringReport`.
- [ ] Customer info: `CustomerInformation` out, `NotifyCustomerInformation` in.
- [ ] Display: `SetDisplayMessage`, `GetDisplayMessages`, `ClearDisplayMessage` + inbound `NotifyDisplayMessages`.
- [ ] Tariff: `CostUpdated` (Block I).
