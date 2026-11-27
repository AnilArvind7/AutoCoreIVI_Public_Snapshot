package com.example.autocorecarsimulator.data

data class VehicleData(
    val speed: Int = 0,
    val battery: Int = 100,
    val ignitionOn: Boolean = false,
    val headlightsOn: Boolean = false,
    val headlightBeam: String = "LOW",
    val hvacOn: Boolean = false,
    val temperature: Int = 22,
    val gear: String = "P",
    val frontLeftDoorOpen: Boolean = false,
    val frontRightDoorOpen: Boolean = false,
    val rearLeftDoorOpen: Boolean = false,
    val rearRightDoorOpen: Boolean = false
)

data class GatewayConnectionState(
    val status: String = "Disconnected",
    val message: String = "Gateway not connected"
)

data class VehicleUiState(
    val vehicleData: VehicleData = VehicleData(),
    val gateway: GatewayConnectionState = GatewayConnectionState()
)
