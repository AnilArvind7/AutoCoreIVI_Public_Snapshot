# AutoCore IVI

**An Android Automotive-style instrument-cluster and infotainment demo**

AutoCore IVI is a portfolio project for exploring vehicle-data flows and Android automotive concepts through a working simulator-to-dashboard prototype. It combines two Android apps, a Python WebSocket gateway, and selectable vehicle-data backends.

> **Scope note:** This is an application-level learning/demo project. The Mock VHAL is implemented in app code; the native C++ store is app-local; and the AAOS path is a client that reads properties through the Android Car API where a compatible AAOS environment and permissions are available. This repository is not a full AOSP/AAOS build, a production VHAL implementation, or a connection to physical vehicle hardware.

## Demo architecture

```text
┌──────────────────────────┐
│ AutoCore Car Simulator   │
│ Android phone app        │
└────────────┬─────────────┘
             │ WebSocket / JSON
             ▼
┌──────────────────────────┐
│ Python WebSocket Gateway │
│ Port 8765                │
└────────────┬─────────────┘
             │ WebSocket / vehicle state
             ▼
┌──────────────────────────────────────────────┐
│ AutoCore IVI Dashboard                       │
│                                              │
│ VehicleRepository + selectable data source   │
│   ├─ Simulation (WebSocket)                  │
│   ├─ Native (app-local C++ property store)   │
│   └─ AAOS (Android Car API client)           │
│                                              │
│ Mock VHAL/property model → service/state → UI│
└──────────────────────────────────────────────┘
```

The simulation source is the end-to-end phone-to-dashboard demo path. The dashboard also includes separate Native and AAOS source options for demonstrating alternative data-source integrations.

## Features

- Live vehicle-state updates from the phone simulator to the IVI dashboard.
- Instrument-cluster and dashboard UI built with Kotlin and Jetpack Compose.
- Simulated speed, battery, ignition, headlight/beam, HVAC/temperature, gear, and individual door states.
- Mock VHAL-style property definitions, IDs, area IDs, configuration/value conversion, and an in-memory property store.
- Vehicle-property service APIs and AIDL interfaces for property reads/writes and change callbacks.
- JNI bridge to an app-local C++ property store for the Native data source.
- Selectable Simulation, Native, and AAOS data sources in the dashboard.
- AAOS client path for reading supported vehicle properties through Car Service on a compatible AAOS device/emulator.
- Unit tests for core property mapping, conversion, Mock VHAL, and service behavior.

## Technology stack

| Area | Technologies |
|---|---|
| Android apps | Kotlin, Jetpack Compose, Android SDK |
| Async/state | Kotlin Coroutines, `StateFlow` |
| Communication | OkHttp WebSocket, JSON |
| Gateway | Python, `websockets` |
| IPC | Android AIDL / Binder (app-level service interfaces) |
| Native | C++, JNI, CMake |
| Automotive concepts | AAOS Car API client, Mock VHAL-style property model |
| Tests | JUnit and Android instrumentation-test dependencies |

## Repository layout

```text
AOSP_Connected_Apps/
├── AutoCoreCarSimulator/       # Phone-side vehicle control simulator
├── AutoCoreIVIDashboard/       # IVI dashboard, data sources, property model, IPC/native code
├── gateway/
│   ├── gateway.py              # WebSocket server and shared vehicle state
│   └── requirements.txt        # Python dependencies
└── README.md
```

## Run the simulation demo

### 1. Start the Python gateway

From the repository root:

```bash
cd gateway
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python gateway.py
```

The gateway listens on port `8765` and broadcasts the latest validated vehicle state to connected clients.

### 2. Configure the Android apps

- Connect the phone and the IVI emulator/device to a network that can reach the computer running the gateway.
- Update the `GATEWAY_URL` constant in each app’s `VehicleGatewayClient.kt` to the gateway computer’s reachable address and port `8765`. The dashboard default `10.0.2.2` is the Android Emulator host bridge; for a physical phone, use the computer’s LAN IP. The simulator default `192.168.1.100` is a placeholder and must be replaced with your gateway computer’s LAN IP.
- Allow the gateway port through the computer's firewall if required.

### 3. Launch and verify

1. Open `AutoCoreCarSimulator` in Android Studio and run it on the phone or a phone emulator.
2. Open `AutoCoreIVIDashboard` and run it on an Android emulator/device.
3. Select the **Simulation** vehicle-data source in the dashboard.
4. Change simulator controls and confirm that the dashboard reflects the updated state.

## Data flow

```text
Simulator controls
  → WebSocket JSON message
  → Python gateway validates and broadcasts state
  → Dashboard WebSocket client
  → VehicleRepository / SimulationVehicleDataSource
  → Mock VHAL property mapping and property service
  → observable vehicle state
  → Compose dashboard UI
```

The AIDL interfaces define property read/write and callback contracts. They demonstrate Binder IPC at the application level; they are not Android platform CarService or VHAL interfaces.

## Vehicle-data source options

- **Simulation:** Receives state from the Python gateway. Use this for the complete phone-to-dashboard demonstration.
- **Native:** Uses JNI to access the dashboard app's local C++ property store. It is an app-local demonstration, not a hardware-backed VHAL.
- **AAOS:** Uses the Android Car API client path to observe supported properties from AAOS Car Service. Availability depends on the target AAOS build, supported properties, and granted permissions; it is not the default simulator transport.

## Development notes

- Open each Android project folder separately in Android Studio.
- Use the SDK and JDK versions required by the Gradle configuration in the project.
- Keep machine-specific `local.properties` files and generated build/IDE files out of commits.
- The gateway's bind address is configured in `gateway.py`; use a reachable host address in the Android apps.

## Current boundaries

- Vehicle values are simulated; no physical vehicle bus or ECU is connected.
- The Mock VHAL models property behavior inside the app and does not replace the platform VHAL service.
- The C++ property store is local to the app process/library and is not an automotive HAL.
- The AAOS path requires a compatible AAOS environment and permissions and may not work on a standard Android emulator.
- This project does not include a complete AOSP source build or production vehicle integration.

## Learning focus

AutoCore IVI is intended to demonstrate practical understanding of Android automotive-style HMI development, vehicle-property modeling, state propagation, WebSocket-based simulation, app-level Binder/AIDL IPC, JNI/C++, and the boundary between application code and platform automotive services.
