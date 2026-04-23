# OCPP 2.0.1 Implementation TODO

Source: OCPP 2.0.1 Edition 4 Errata February 2026 (NotebookLM).
Last audit: 2026-04-23.
Phase 0 completed: 2026-04-21 (all subsections done — separation groundwork
plus full response-schema authoring and hard-reject validation, see 0.4).
Phase 4 (Block P / DataTransfer) completed: 2026-04-22.
Phase 5 (Blocks E, H, K — Reservations, Transactions, Smart Charging) completed: 2026-04-23.
Phase 6 (Blocks L, N — Firmware & Diagnostics) completed: 2026-04-23.

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
- [x] Outbound: `RequestStartTransaction` — `RequestStartTransactionUseCaseV201` under `usecases/v201/`; builds `{remoteStartId, idToken:{idToken,type,additionalInfo?}, evseId?, groupIdToken?}` and returns typed `RequestStartTransactionResult`. `chargingProfile` deferred to Phase 5 alongside `SetChargingProfile`.
- [x] Outbound: `RequestStopTransaction` — `RequestStopTransactionUseCaseV201` under `usecases/v201/`; builds `{transactionId}` (string UUID) and returns typed `RequestStopTransactionResult` (status + optional `statusInfo.reasonCode`).
- [x] Outbound: `TriggerMessage` — `TriggerMessageUseCaseV201` under `usecases/v201/`; full 11-value `MessageTrigger` enum (incl. ISO 15118 cert signing + `PublishFirmwareStatusNotification`); optional `Evse` target; typed `TriggerMessageResult` with `Accepted`/`Rejected`/`NotImplemented`. New `TriggerMessageRequest`/`Response` schemas on classpath.
- [x] Outbound: `UnlockConnector` — `UnlockConnectorUseCaseV201` under `usecases/v201/`; builds `{evseId, connectorId}` (both required, spec > 0) and returns typed `UnlockConnectorResult` with the 4-valued `UnlockStatus` (`Unlocked` / `UnlockFailed` / `OngoingAuthorizedTransaction` / `UnknownConnector`). New `UnlockConnectorRequest`/`Response` schemas on classpath.

## Phase 3 — Availability, Auth, Local List (Blocks C, D, G)
- [x] Outbound: `ChangeAvailability` — `ChangeAvailabilityUseCaseV201` under `usecases/v201/`; three targeting levels (whole station / whole EVSE / specific connector) via optional `Evse` locator; `OperationalStatus` + `ChangeAvailabilityStatus` enums (incl. `Scheduled` for transaction-in-progress deferral per G03.FR.05 / G04.FR.06). New `ChangeAvailabilityRequest`/`Response` schemas on classpath.
- [x] Outbound: `ClearCache` — `ClearCacheUseCaseV201` under `usecases/v201/`; empty request, typed `ClearCacheResult` (`Accepted`/`Rejected`) with `statusInfo.reasonCode` surfacing the `AuthCacheDisabled` / technical-failure distinction per C11.FR.04 / C11.FR.05. New `ClearCacheRequest`/`Response` schemas on classpath.
- [x] Outbound: `GetLocalListVersion` — `GetLocalListVersionUseCaseV201` under `usecases/v201/`; empty request, typed `GetLocalListVersionResult` carrying the integer `versionNumber` (response uses `versionNumber`, not v1.6's `listVersion`). `versionNumber == 0` signals "no list installed" per D02.FR.02 / D02.FR.03; negative values are rejected. New `GetLocalListVersionRequest`/`Response` schemas on classpath.
- [x] Outbound: `SendLocalList` — `SendLocalListUseCaseV201` under `usecases/v201/`; carries `versionNumber` (enforced `> 0` per D01.FR.18), `UpdateType` (`Full`/`Differential`), and a `List<AuthorizationData>` where per-entry `IdTokenInfo` presence/absence drives add-vs-remove in differential mode (D01.FR.16 / D01.FR.17). New `AuthorizationStatus` (10 values), `MessageFormat`, `MessageContent`, `IdTokenInfo`, `AuthorizationData`, `UpdateType` domain types under `domain/v201/model/`. Shared `IdTokenWire` codec extracted so the v2.0.1 `IdToken` / `IdTokenInfo` wire shape is emitted identically by every outbound use case (RequestStartTransaction migrated in this commit). Typed `SendLocalListResult` with `Accepted` / `Failed` / `VersionMismatch`. New `SendLocalListRequest`/`Response` schemas on classpath.

## Phase 4 — Generic Extension Point (Block P)
- [x] Inbound `DataTransferHandler201` + use case + schemas — `HandleDataTransferUseCaseV201` + `DataTransferHandler201` under `handler/v201/`; v201-scoped `DataTransferStatus` / `DataTransferResult` DTOs; `data` is anyType (Jackson `JsonNode` at the wire, plain `Object` in the domain port). Default vendor allow-list is empty, so every CS→CSMS request resolves to `UnknownVendorId` per P02.FR.06; known vendors fall through to `Accepted`. New `DataTransferRequest`/`Response` schemas on classpath.
- [x] Outbound `DataTransferUseCaseV201` + schemas — `SendDataTransferUseCaseV201` under `usecases/v201/` against `Ocpp201StationCommandSender`. Accepts primitives/lists/maps for the opaque `data` field, parses the 4-valued `DataTransferStatus` plus optional `statusInfo.reasonCode`, and surfaces the station's response `data` back through `DataTransferResult.data()`. Request/Response schemas are shared with the inbound path.

## Phase 5 — Reservations, Transactions, Smart Charging (Blocks E, H, K) ✅
- [x] Outbound: `ReserveNowUseCaseV201` + `CancelReservationUseCaseV201` under `usecases/v201/` — v2.0.1 shapes: `IdToken` (via `IdTokenWire`), optional `evseId` + `connectorType` + `groupIdToken`. New `ConnectorType` enum (25 spec values) and `ReserveNowStatus` (5 values) / `CancelReservationStatus` (2 values). `ReservationCommand201IT` covers Block H.
- [x] Outbound: `GetTransactionStatusUseCaseV201` (E14, 2.0.1-only) — null-preserving `ongoingIndicator` so callers can tell "absent" from "false" per E14.FR.06. `TransactionCommand201IT`.
- [x] Outbound: `SetChargingProfileUseCaseV201` / `ClearChargingProfileUseCaseV201` / `GetCompositeScheduleUseCaseV201` — new `domain/v201/smartcharging/` package with `ChargingProfile` + `ChargingSchedule` + `ChargingSchedulePeriod` records (cross-field K01 rules enforced at construction: Recurring⇒recurrencyKind, Relative⇒no startSchedule, TxProfile⇔transactionId, first period startPeriod==0), plus purpose/kind/recurrency/rate-unit enums. `CompositeSchedule` is a distinct record from `ChargingSchedule` (no id/minChargingRate/salesTariff). `ChargingProfileWire` codec carries `toWire`/`fromWire` for the whole tree.
- [x] Outbound: `GetChargingProfilesUseCaseV201` (K09, 2.0.1-only) with new `ChargingProfileCriterion` (limit-source + id arrays + purpose + stackLevel) and `ChargingLimitSource` enum. 2-valued `GetChargingProfilesStatus` (Accepted/NoProfiles).
- [x] Inbound: `ReportChargingProfilesHandler201` (K09 follow-up) + `NotifyChargingLimitHandler201` + `ClearedChargingLimitHandler201`. Added `ChargingLimit` domain record (source + optional isGridCritical). All three handlers are pass-through use cases with `Sink` functional-interface ports. `ReportChargingProfiles201IT` + `ChargingLimit201IT`.
- [x] Inbound: `NotifyEVChargingNeedsHandler201` + `NotifyEVChargingScheduleHandler201` (ISO 15118). New domain types `EnergyTransferMode` (4 values incl. underscored AC variants), `AcChargingParameters`, `DcChargingParameters` (with SoC-percentage validators), `ChargingNeeds` aggregate. Both use cases take a caller-supplied `Policy` functional interface because these messages have non-empty responses (`NotifyEVChargingNeedsStatus` / `GenericStatus`). `EVCharging201IT`.

Wire codec centralisation: `ChargingProfileWire` (under `smartcharging/wire/`) grew to cover every smart-charging type's toWire/fromWire. Mirrors the `DeviceModelWire` pattern.

Phase 5 tally: 7 new outbound use cases, 5 new inbound handlers, ~19 new schema files; 129 integration tests in `adapter-ocpp-ws` (up from 113). Smart-charging domain package bootstrapped for Phase 8+ to extend (monitoring, display messages).

### Phase 5 audit follow-ups (2026-04-23)
- [x] K01.FR.22: `SetChargingProfileUseCaseV201` now rejects `purpose=ChargingStationExternalConstraints` (spec says CSMS MUST NOT send it).
- [x] K01.FR.35: `ChargingSchedule` now validates `chargingSchedulePeriod` entries are ordered by strictly increasing `startPeriod`.
- [x] K01.FR.38: `ChargingProfile` now rejects the `ChargingStationMaxProfile` + `Relative` combination at construction.
- [x] K09.FR.03: `GetChargingProfilesUseCaseV201` rejects empty criteria; the spec-violating `ChargingProfileCriterion.all()` factory was dropped in favour of an `isEmpty()` guard.

## Phase 6 — Firmware & Diagnostics (Blocks L, N) ✅
- [x] Inbound: `FirmwareStatusNotification` — `FirmwareStatusNotificationHandler201` + `HandleFirmwareStatusNotificationUseCaseV201` (Sink). New `FirmwareStatus` enum (14 spec values) with `FirmwareWire` codec under `domain/v201/firmware/`. `requestId` optional per L01.FR.20 (only required when status≠Idle).
- [x] Outbound: `UpdateFirmware` — `UpdateFirmwareUseCaseV201`. New `Firmware` record (location + retrieveDateTime required, optional installDateTime / signingCertificate / signature with maxLength 512 / 5500 / 800). `UpdateFirmwareStatus` enum (5 values, AcceptedCanceled folded into `isAccepted()` per L01.FR.34). Typed `UpdateFirmwareResult` with optional `statusInfo.reasonCode`.
- [x] Inbound: `LogStatusNotification` — `LogStatusNotificationHandler201` + `HandleLogStatusNotificationUseCaseV201` (Sink). New `UploadLogStatus` enum (8 spec values incl. `AcceptedCanceled` for N01.FR.12) with `DiagnosticsWire` codec under a new `domain/v201/diagnostics/` subpackage. `requestId` optional per N01.FR.13.
- [x] Outbound: `GetLog` — `GetLogUseCaseV201`. New `LogParameters` record (remoteLocation required, optional time window with oldest≤latest validation), 2-valued `LogType` enum (`DiagnosticsLog` / `SecurityLog`) and 3-valued `GetLogStatus` enum. Typed `GetLogResult` surfaces optional `filename` + `statusInfo.reasonCode`. AcceptedCanceled folded into `isAccepted()` per N01.FR.12.
- [x] Inbound: `NotifyEvent` — `NotifyEventHandler201` + `HandleNotifyEventUseCaseV201` (Sink). New `EventData` record (7 required fields + 7 optional: cause / techCode / techInfo / cleared / transactionId / variableMonitoringId), `EventTrigger` (3 values), `EventNotificationType` (4 values). Reuses devicemodel `Component` / `Variable` types via `DeviceModelWire` so the device-model and event paths emit the same shape. Multi-frame batches arrive as separate sink calls keyed by ascending `seqNo` per N08.FR.04.
- [x] Inbound/Outbound: `PublishFirmware` + `PublishFirmwareStatusNotification` — `PublishFirmwareUseCaseV201` (outbound, GenericStatus response, 32-char MD5 checksum length enforced) + `PublishFirmwareStatusNotificationHandler201` (inbound, 10-value `PublishFirmwareStatus` enum). Use case enforces L03.FR.04 — `status=Published` MUST come with at least one location URI.

Phase 6 tally: 4 new outbound use cases (UpdateFirmware, GetLog, PublishFirmware + the existing pattern), 4 new inbound handlers (FirmwareStatusNotification, LogStatusNotification, NotifyEvent, PublishFirmwareStatusNotification), ~12 new schema files; 147 integration tests in `adapter-ocpp-ws` (up from 129 at end of Phase 5). New `domain/v201/firmware/` and `domain/v201/diagnostics/` packages bootstrap Phase 7 (Security/Certificates) and Phase 8 (Monitoring/Display) — `EventData` will extend naturally to `NotifyMonitoringReport`, and the firmware update/security flows already share the StatusInfo shape used by certificate ops.

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
