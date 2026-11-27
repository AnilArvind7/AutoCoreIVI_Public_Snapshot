# AutoCore IVI

**A Real-Time Android Automotive-Style Infotainment Demo**

AutoCore IVI is a demonstration project exploring vehicle-data communication between an Android-based car simulator and an infotainment dashboard using a Python WebSocket gateway.

## Architecture

```text
Android Car Simulator
 (Physical Android Phone)
          |
          | WebSocket
          v
   Python Gateway
    (Port 8765)
          |
          | WebSocket
          v
 Android IVI Dashboard
 (Automotive Emulator)
```

## Features

- Real-time simulated vehicle-data updates.
- Speed and battery-level monitoring.
- Ignition and headlight status.
- HVAC controls and temperature updates.
- Kotlin and Jetpack Compose Android interfaces.
- Python WebSocket gateway for communication between apps.

## Technology Stack

| Component | Technology |
|---|---|
| Android applications | Kotlin |
| User interfaces | Jetpack Compose |
| WebSocket client | OkHttp |
| Gateway | Python, `websockets` |
| Development environment | Android Studio |
| Target exploration | Android Automotive / AOSP |

## Project Structure

```text
AOSP_Connected_Apps/
├── AutoCoreCarSimulator/
├── AutoCoreIVIDashboard/
├── gateway/
│   ├── gateway.py
│   └── requirements.txt
└── README.md
```

## Running the Demo

1. Start the Python gateway on your development machine.
2. Connect your Android phone and Automotive emulator to the same reachable network.
3. Configure both applications to use the development machine's IP address and gateway port `8765`.
4. Launch the car simulator on the phone.
5. Launch the IVI dashboard on the emulator.
6. Change vehicle controls in the simulator and observe updates on the dashboard.

See `gateway/requirements.txt` for the Python dependency version.

## Automotive Learning Objectives

This project is intended to explore:

- Vehicle-data communication and UI integration.
- Android Automotive-style infotainment interfaces.
- AIDL and Binder-based service communication as future extensions.
- The Android Automotive vehicle abstraction layer (VHAL) and AOSP architecture as future learning milestones.

## Current Scope

**This is a prototype using simulated vehicle data.** It does not currently implement a real Android Automotive OS VHAL connection or communicate with physical vehicle hardware.

## Future Improvements

- Connection recovery and stale-data handling.
- Configurable gateway address and connection settings.
- AIDL/Binder-based vehicle-data service.
- Integration experiments with AOSP and VHAL.
- Demo screenshots and architecture documentation.
