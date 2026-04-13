# Reference Charger Simulators

Uses the [SAP e-mobility-charging-stations-simulator](https://github.com/SAP/e-mobility-charging-stations-simulator) as a reference OCPP charger client to test the evlibre CSMS.

## Setup

Runs 4 simulated chargers against your local evlibre server:
- 2x OCPP 1.6 stations (`SAP-16-*`)
- 2x OCPP 2.0.1 stations (`SAP-201-*`)

## Usage

1. Start the evlibre CSMS server (port 9090)
2. Build and run the reference simulator:

```bash
cd reference-simulators
docker compose up --build
```

3. Watch your server logs for connections from `SAP-16-*` and `SAP-201-*` stations

## Configuration

- `sap-simulator/config.json` — main config (supervision URL, station counts)
- `sap-simulator/station-16.json` — OCPP 1.6 station template
- `sap-simulator/station-201.json` — OCPP 2.0.1 station template
- `sap-simulator/idtags.json` — RFID tags for authorization (must match server's valid tags)

## What it tests

The SAP simulator automatically runs full charging sessions:
- BootNotification → Heartbeat → StatusNotification → Authorize → StartTransaction → MeterValues → StopTransaction (OCPP 1.6)
- BootNotification → Heartbeat → StatusNotification → Authorize → TransactionEvent → MeterValues (OCPP 2.0.1)

## Networking

- **macOS (Docker Desktop):** Uses `host.docker.internal` to reach localhost:9090 (works out of the box)
- **Linux:** Add `network_mode: host` to `docker-compose.yml` and change URLs to `ws://localhost:9090/...`
