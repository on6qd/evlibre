# OCPP 2.0.1 Implementation TODO

Source: OCPP 2.0.1 Edition 4 Errata February 2026 (NotebookLM).
Last audit: 2026-04-24.
Phase 0 completed: 2026-04-21 (all subsections done — separation groundwork
plus full response-schema authoring and hard-reject validation, see 0.4).
Phase 4 (Block P / DataTransfer) completed: 2026-04-22.
Phase 5 (Blocks E, H, K — Reservations, Transactions, Smart Charging) completed: 2026-04-23.
Phase 6 (Blocks L, N — Firmware & Diagnostics) completed: 2026-04-23.
Phase 7 (Blocks A, M — Security & Certificates) completed: 2026-04-24.
Phase 8 (Block N monitoring + Blocks O, I — Monitoring, Customer/Display, Tariff) completed: 2026-04-24. **All planned phases are now complete.**
0.1 StatusNotification/MeterValues v201 reshape (the last deferred item) closed 2026-04-24.

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
- [x] Reshape `StatusNotificationData` / `MeterValuesData` per protocol (2026-04-24): new `StatusNotificationData201` (evseId + connectorId + status + timestamp — drops 1.6-only info/vendorId/vendorErrorCode/errorCode) and `MeterValuesData201` (EvseId addressing, no transactionId — that flows via `TransactionEvent` in v2.0.1) live under `domain/v201/dto/`; v201 ports, use cases, and handlers are now wired to them. No v201 path references a v16-shaped DTO.

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

### Phase 6 audit follow-ups (2026-04-23)
- [x] L01.FR.11 + L01.FR.12: `Firmware` now rejects lopsided `signingCertificate` / `signature` pairs (either both present for a secure update, or both absent for L02). Covered by new `FirmwareTest`.
- [x] L03.FR.02: `PublishFirmwareUseCaseV201` now enforces checksum is a 32-char hex string (regex), matching the spec's "hexadecimal string of length 32" contract — not just `length == 32`. Also cleaned up the put-then-remove payload builder to the conditional-put pattern used by `UpdateFirmwareUseCaseV201`. Covered by new `PublishFirmwareUseCaseV201Test`.

## Phase 7 — Security & Certificates (Blocks A, M) ✅
- [x] Inbound: `SecurityEventNotification` — `SecurityEventNotificationHandler201` + `HandleSecurityEventNotificationUseCaseV201` (Sink). New `SecurityEvent` record under `domain/v201/security/` enforcing spec maxLengths (type 50, techInfo 255). Empty ack response.
- [x] Inbound: `SignCertificate` — `SignCertificateHandler201` + `HandleSignCertificateUseCaseV201` (Policy pattern — caller decides Accept/Reject). New `CertificateSigningUse` enum (ChargingStationCertificate / V2GCertificate) with absent-defaults-to-CS per the spec; csr maxLength 5500 enforced. Typed `SignCertificateResult` carries optional `statusInfo.reasonCode`.
- [x] Outbound: `GetInstalledCertificateIds` — `GetInstalledCertificateIdsUseCaseV201`. New cert-management domain vocabulary under `domain/v201/security/`: `HashAlgorithm` (3 values), `GetCertificateIdUse` (5 values — superset of InstallCertificateUse because V2GCertificateChain is discoverable but not installable), `CertificateHashData` (spec maxLengths 128/128/40 enforced at construction), `CertificateHashDataChain` (leaf + optional children). `SecurityWire` centralises the wire codec. 2-valued status (Accepted / NotFound).
- [x] Outbound: `InstallCertificate` — `InstallCertificateUseCaseV201`. New `InstallCertificateUse` enum (strict subset of `GetCertificateIdUse` — no V2GCertificateChain; it's discoverable only, never directly installed). Tri-state `InstallCertificateStatus` (Accepted / Rejected / Failed) distinguishing "station policy-denied" from "chain failed verification". Certificate PEM maxLength 5500.
- [x] Outbound: `DeleteCertificate` — `DeleteCertificateUseCaseV201`. Reuses the `CertificateHashData` + `SecurityWire.certificateHashDataToWire` codec from 7.3 so the wire tuple is emitted identically by every cert-management use case. Tri-state `DeleteCertificateStatus` (Accepted / Failed / NotFound).
- [x] Inbound: `GetCertificateStatus` — `GetCertificateStatusHandler201` + `HandleGetCertificateStatusUseCaseV201` (OcspResolver policy). New `OcspRequestData` record (reuses `HashAlgorithm`; adds `responderURL` maxLength 512). Typed `GetCertificateStatusResult` with `accepted(ocspResult)` / `failed(reasonCode)` factories; ocspResult maxLength 5600 enforced. Required for ISO 15118 Plug & Charge so stations can verify EV contract certs without direct CA OCSP network access.
- [x] Inbound: `Get15118EVCertificate` — `Get15118EVCertificateHandler201` + `HandleGet15118EVCertificateUseCaseV201` (ExiProcessor policy). New `CertificateAction` enum (Install / Update). 2-valued `Iso15118EVCertificateStatus`; `exiResponse` is REQUIRED on the wire even for Failed so the station can forward a valid EXI ResponseCode back to the EV. `iso15118SchemaVersion` maxLength 50; `exiRequest` / `exiResponse` maxLength 5600.

Phase 7 tally: 3 new outbound use cases (GetInstalledCertificateIds, InstallCertificate, DeleteCertificate), 4 new inbound handlers (SecurityEventNotification, SignCertificate, GetCertificateStatus, Get15118EVCertificate); ~14 new schema files; 162 integration tests in `adapter-ocpp-ws` (up from 147 at end of Phase 6). New `domain/v201/security/` package bootstraps the certificate vocabulary (HashAlgorithm, CertificateHashData, the Use enums, OcspRequestData) that future work on block A05 (`CertificateSigned` outbound) and A08 (`PublishFirmware` security extensions) will extend naturally.

### Phase 7 audit follow-ups (2026-04-24)
- [x] A04: `GetCertificateStatusResult` now enforces the schema's "ocspResult MAY only be omitted when status is not Accepted" at construction — a non-blank `ocspResult` is required when status=Accepted. Covered by new `GetCertificateStatusResultTest`.
- [x] M03: `CertificateHashDataChain` now enforces `childCertificateHashData` maxItems 4 in the record (schema already enforced on the wire). Covered by new cases in `CertificateHashDataTest`.

## Phase 8 — Monitoring & Display (Blocks N, O, I) ✅
Largely 2.0.1-only features; first phase with zero v1.6 siblings at all.

### Block N — Variable monitoring
- [x] Domain: `MonitorType` (5 spec values), `VariableMonitor` (id,
  transactionOnly, value, type, severity) with 0-9 severity bounds.
  Wire helpers added to `DeviceModelWire` for both types + the nested
  VariableMonitor entry.
- [x] Outbound: `SetVariableMonitoringUseCaseV201` — `SetMonitoringData`
  create-vs-replace via nullable `id`, 6-valued `SetMonitoringStatus`,
  per-entry result with id-only-on-accepted.
- [x] Outbound: `ClearVariableMonitoringUseCaseV201` — id array in, per-id
  `ClearMonitoringResult` out with 3-valued status (Accepted / Rejected /
  NotFound) so callers can tell which specific ids failed.
- [x] Outbound: `SetMonitoringBaseUseCaseV201` — 3-valued
  `MonitoringBase` (All / FactoryDefault / HardWiredOnly). Introduced the
  shared `GenericDeviceModelStatus` enum (4 values) for this and for
  GetMonitoringReport / future GetBaseReport / GetReport migrations.
- [x] Outbound: `SetMonitoringLevelUseCaseV201` — severity threshold
  0-9, GenericStatus response. Severity bounds pulled from
  `VariableMonitor.SEVERITY_MIN/MAX` so the 0-9 range has one source of
  truth across SetVariableMonitoring + SetMonitoringLevel.
- [x] Outbound: `GetMonitoringReportUseCaseV201` — new
  `MonitoringCriterion` enum (3 values — Threshold / Delta / Periodic),
  reuses existing `ComponentVariableSelector` for the component/variable
  filter.
- [x] Inbound: `NotifyMonitoringReportHandler201` + buffering use case +
  `MonitorRepositoryPort` / `NotifyMonitoringReportCompletionPort` +
  `InMemoryMonitorRepository`. `MonitoringReporting201IT` covers the full
  GetMonitoringReport → multi-frame NotifyMonitoringReport → repo-upsert
  + completion-event loop.

### Block O — Customer info + Display messages
- [x] Outbound: `CustomerInformationUseCaseV201`. Bundles the three
  mutually-optional identifiers (`customerIdentifier` string maxLength 64,
  `IdToken`, `CertificateHashData`) as `CustomerInformationTarget` with
  `none()` / `byIdentifier()` / `byIdToken()` / `byCertificate()`
  factories; 3-valued `CustomerInformationStatus` (Accepted / Rejected /
  Invalid — Invalid is distinct: format-not-recognised vs general refusal).
  Reuses existing `IdTokenWire.toWire` and `SecurityWire.certificateHashDataToWire`.
- [x] Inbound: `NotifyCustomerInformationHandler201` + buffering use case +
  `CustomerInformationSink` (functional interface). Scalar-string
  buffering via StringBuilder per (tenant, station, requestId); empty
  single frames still fire the sink with `""` so "no data" is
  distinguishable from "no response". `CustomerInformation201IT` covers
  CustomerInformation(report=true) → 3-frame NotifyCustomerInformation →
  single sink call with concatenated string.
- [x] Outbound: `SetDisplayMessageUseCaseV201`. Bootstraps
  `domain/v201/displaymessage/` — `MessagePriority` (3 values),
  `MessageState` (4 values), `MessageInfo` record (id + priority +
  message required; state, display Component, start/end window,
  transactionId maxLength 36 optional), `SetDisplayMessageStatus` (6
  spec values). New `DisplayMessageWire` codec. Reuses existing
  `MessageContent`/`MessageFormat` from `model/`.
- [x] Outbound: `GetDisplayMessagesUseCaseV201` — optional id array +
  MessagePriority + MessageState filter; 2-valued
  `GetDisplayMessagesStatus`.
- [x] Outbound: `ClearDisplayMessageUseCaseV201` — single id in,
  2-valued `ClearMessageStatus` (Accepted / Unknown) out.
- [x] Inbound: `NotifyDisplayMessagesHandler201` + buffering use case +
  `DisplayMessagesSink` (functional interface). Narrower schema than
  NotifyReport — no seqNo/generatedAt on the wire — but same multi-frame
  semantics. `DisplayMessages201IT` covers GetDisplayMessages → 2-frame
  NotifyDisplayMessages → single sink call with 3 aggregated
  MessageInfos.

### Block I — Tariff
- [x] Outbound: `CostUpdatedUseCaseV201` — (totalCost, transactionId),
  empty-response spec so port returns `CompletableFuture<Void>`.
  transactionId maxLength 36, negative totalCost allowed for refund
  scenarios.

Phase 8 tally: 10 new outbound use cases (SetVariableMonitoring,
ClearVariableMonitoring, SetMonitoringBase, SetMonitoringLevel,
GetMonitoringReport, CustomerInformation, SetDisplayMessage,
GetDisplayMessages, ClearDisplayMessage, CostUpdated), 3 new inbound
handlers (NotifyMonitoringReport, NotifyCustomerInformation,
NotifyDisplayMessages) with associated buffering use cases + repositories
/ sinks; ~20 new schema files; 508 core tests (up from 416 at end of
Phase 7) and 168 adapter-ocpp-ws tests (up from 162). New
`domain/v201/displaymessage/` package; `domain/v201/devicemodel/`
extended with the deferred `Monitor` shape from Phase 1.
