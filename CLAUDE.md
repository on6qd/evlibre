# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development process

### Incremental implementation

When implementing any plan, work in small increments. Each increment must satisfy **all** of the following before moving on:

1. The project compiles (`mvn compile` / `mvn test-compile` passes).
2. The new functionality introduced in this increment is covered by unit tests and/or integration tests, and those tests pass.
3. The increment is committed as its own git commit, following the existing commit-message conventions (see `git log`).

Do not start the next increment until the current one is compiled, tested, and committed. No "I'll add tests at the end"; no "let me get it all working first, then split commits". If an increment grows too large to test cleanly, split it further rather than deferring tests.

Break multi-step plans (e.g. a phase from `OCPP201_TODO.md`) into the smallest shippable units — typically one domain class, one use case, one handler, one port, or one schema set per increment. Prefer the narrowest `-pl` Maven subset that covers the change when verifying locally.

## Build, test, run

Java 17 + Maven multi-module. Surefire runs unit tests (`*Test.java`); failsafe runs integration tests (`*IT.java` tagged `@Tag("integration")`). CI runs `mvn -B -ntp verify` (see `.github/workflows/ci.yml`).

```bash
mvn verify                                   # full suite: units + ITs (what CI runs)
mvn test                                     # units only
mvn -pl server/core -am test                 # narrow scope to one module + its deps
mvn -pl server/adapter-ocpp-ws -am verify    # run the OCPP ITs in-process
mvn -pl <module> test -Dtest=ClassName       # single test class
mvn -pl <module> test -Dtest=ClassName#method

mvn -q clean package -DskipTests             # fat jar
java -jar server/application/target/application-1.0-SNAPSHOT.jar
# Optional: pass `--config /path/to/server.toml`. OCPP WS on 9090, Web UI on 8080.
```

Reference simulator (SAP) for end-to-end acceptance lives in `reference-simulators/`; run with `docker compose up --build` while the CSMS is running locally. See `TESTPLAN.md` for the full strategy.

## Architecture — the big picture

### Hexagonal core + adapters

`server/core` is the pure domain. Two top-level packages:

- `domain/` — entities, value objects, inbound ports, outbound ports.
- `usecases/` — application logic that orchestrates ports.

No frameworks, no JSON, no WebSocket code in `core`. Adapters live in their own modules and depend on `core`:

- `adapter-ocpp-ws` — Vert.x WebSocket verticle, OCPP codec/dispatcher/schema validator, inbound handlers, outbound command sender.
- `adapter-ui-web` — Vert.x HTTP verticle serving the operator Web UI.
- `adapter-persistence-inmemory`, `adapter-persistence-h2` — alternative implementations of the repository ports. H2 variant uses Flyway migrations.
- `application` — composition root; wires verticles, adapters, and config (`server.toml`) together and deploys on Vert.x.
- `test-support` — shared fakes and fixtures for tests.
- `common` — protocol-neutral identity/enum types (`ChargePointIdentity`, `ConnectorId`, `EvseId`, `OcppProtocol`, `MessageTypeId`).

### Strict OCPP 1.6 / 2.0.1 separation (load-bearing rule)

`domain/` and `usecases/` are each split three ways: `shared/`, `v16/`, `v201/`. Protocol-specific code does **not** cross that line:

- **Shared**: `ChargingStation` identity, `Tenant`, negotiated `OcppProtocol`, protocol-neutral enums that genuinely match both specs (`RegistrationStatus`, `ConnectorStatus`, `TransactionStatus`), `TimeProvider`, `StationRepositoryPort`.
- **Split per protocol** (no reuse across versions): domain models, DTOs, repository ports, use cases, inbound handlers, outbound command senders, JSON schemas under `adapter-ocpp-ws/src/main/resources/schemas/ocpp16|ocpp201/`.
- **No protocol branching inside a use case.** If the concept differs in shape between versions, it gets two classes.
- **Ports are protocol-scoped**: `OcppStationCommandSender` exposes `.v16()` and `.v201()` accessors; each rejects calls against a session negotiated for the other protocol.

Three layers enforce this — a PR that blurs the separation should trip at least one:

1. Package layout — cross-version references stand out as wrong-package imports.
2. `OcppStationCommandSenderTest` — unit-tests the outbound protocol guard.
3. `Ocpp201HandlersIT` — cross-protocol negative tests at the dispatcher (v1.6 action on a v2.0.1 session returns `NotImplemented`, and vice-versa).

Before implementing any OCPP behaviour, check the relevant spec first (use the `notebooklm` skill — OCPP 1.6 and 2.0.1 Edition 4 Errata notebooks are preloaded). CitrineOS is the behavioural reference when the spec leaves room for interpretation.

### OCPP message path

Inbound (station → CSMS): WebSocket → `OcppMessageCodec` → `OcppMessageDispatcher` (keys on `protocol:action`) → a `handler/v16/*` or `handler/v201/*` class → the matching use case in `usecases/v16` or `usecases/v201` → outbound ports (repositories, event log).

Outbound (CSMS → station): use case → `OcppStationCommandSender.v16()/.v201()` → `OcppPendingCallManager` (tracks in-flight CALL/CALL_RESULT correlation) → WebSocket.

`OcppSchemaValidator` is wired at four checkpoints (inbound CALL, our CALL_RESULT, outbound CALL, station CALL_RESULT). The validator's contract is "missing schema = valid", so partial coverage doesn't block non-wired actions. Validation failures are hard rejections as of Phase 0.4c.

### Concurrency model

Vert.x event-loop. Verticles are single-threaded within themselves; repositories and the pending-call manager must be safe for concurrent verticle deployments. Do not block the event loop — use Vert.x futures / `CompletableFuture` for async work.

### Tests — where to look

- `server/adapter-ocpp-ws/src/test/java/.../testutil/OcppTestHarness` — starts the full OCPP stack in-process against test fakes; every integration test in that module builds on it.
- `server/test-support/` — shared fakes (time, repositories, event log) and fixtures.
- `CoreProfileCommandIT`, `Ocpp201HandlersIT`, `OcppAllHandlersIT`, `OcppChargingSessionIT`, and friends under `adapter-ocpp-ws` — pattern to follow when adding new handler coverage.

## Project state pointers

- `OCPP201_TODO.md` — phase-by-phase plan for OCPP 2.0.1 feature parity. Phase 0 (separation groundwork) is complete as of 2026-04-22. Phase 1 (Device Model + Core Provisioning outbound) is the current unstarted frontier.
- `TESTPLAN.md` — authoritative test strategy, current test counts, manual scenarios, and gaps.
