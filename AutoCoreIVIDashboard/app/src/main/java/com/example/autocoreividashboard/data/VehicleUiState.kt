package com.example.autocoreividashboard.data

data class VehicleUiState(
    val gatewayStatus: String = "Connecting",
    val gatewayMessage: String = "Waiting for vehicle data",
    val speed: Int? = null,
    val battery: Int? = null,
    val ignitionOn: Boolean = false,
    val parkingBrakeOn: Boolean = false,
    val headlightsOn: Boolean = false,
    val headlightBeam: String = "LOW",
    val hvacOn: Boolean = false,
    val temperature: Int? = null,
    val gear: String = "P",
    val frontLeftDoorOpen: Boolean = false,
    val frontRightDoorOpen: Boolean = false,
    val rearLeftDoorOpen: Boolean = false,
    val rearRightDoorOpen: Boolean = false
)
