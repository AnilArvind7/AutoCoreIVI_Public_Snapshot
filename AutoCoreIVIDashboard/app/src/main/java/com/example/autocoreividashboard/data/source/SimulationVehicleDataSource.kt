package com.example.autocoreividashboard.data.source

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyManager
import com.example.autocoreividashboard.data.VehiclePropertyStore
import com.example.autocoreividashboard.data.VehicleUiState
import com.example.autocoreividashboard.socket.VehicleGatewayClient
import com.example.autocoreividashboard.socket.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SimulationVehicleDataSource : VehicleDataSource {

    override val sourceType = VehicleDataSourceType.SIMULATION

    private val _vehicleState = MutableStateFlow(VehicleUiState())
    override val vehicleState: StateFlow<VehicleUiState> =
        _vehicleState.asStateFlow()

    private val propertyManager = VehiclePropertyStore.manager
    override val vehicleProperties =
        propertyManager.observeProperties()

    private val sourceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var retryJob: Job? = null
    private var retryAttempt = 0

    @Volatile
    private var shouldReconnect = false

    private val gatewayClient = VehicleGatewayClient(
        onConnected = {
            retryAttempt = 0
            retryJob?.cancel()
            retryJob = null

            _vehicleState.value = _vehicleState.value.copy(
                gatewayStatus = "Connected",
                gatewayMessage = "Receiving vehicle data"
            )
        },
        onStateReceived = { state ->
            state.updateProperties()
            _vehicleState.value = state.toUiState()
        },
        onError = { message ->
            if (shouldReconnect) {
                _vehicleState.value = _vehicleState.value.copy(
                    gatewayStatus = "Reconnecting",
                    gatewayMessage = message
                )
                scheduleReconnect()
            }
        },
        onDisconnected = {
            if (shouldReconnect) {
                _vehicleState.value = _vehicleState.value.copy(
                    gatewayStatus = "Reconnecting",
                    gatewayMessage = "Gateway connection closed"
                )
                scheduleReconnect()
            }
        }
    )

    override fun connect() {
        shouldReconnect = true
        gatewayClient.connect()
    }

    override fun disconnect() {
        shouldReconnect = false
        retryJob?.cancel()
        retryJob = null
        retryAttempt = 0

        gatewayClient.disconnect()

        _vehicleState.value = _vehicleState.value.copy(
            gatewayStatus = "Disconnected",
            gatewayMessage = "Simulation source stopped"
        )
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || retryJob?.isActive == true) return

        val delaySeconds = (2L shl retryAttempt).coerceAtMost(30L)
        retryAttempt++

        retryJob = sourceScope.launch {
            for (remaining in delaySeconds downTo 1) {
                if (!shouldReconnect) return@launch

                _vehicleState.value = _vehicleState.value.copy(
                    gatewayStatus = "Reconnecting",
                    gatewayMessage = "Retrying in ${remaining}s"
                )

                delay(1000.milliseconds)
            }

            if (shouldReconnect) {
                gatewayClient.connect()
            }
        }
    }

    private fun VehicleState.updateProperties() {
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.SPEED, speed)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.BATTERY_LEVEL, battery)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.IGNITION_STATE, ignitionOn)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HEADLIGHT_STATE, headlightsOn)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HEADLIGHT_BEAM, headlightBeam)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HVAC_STATE, hvacOn)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.CABIN_TEMPERATURE, temperature)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.GEAR_SELECTION, gear)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.FRONT_LEFT_DOOR, frontLeftDoorOpen)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.FRONT_RIGHT_DOOR, frontRightDoorOpen)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.REAR_LEFT_DOOR, rearLeftDoorOpen)
        )
        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.REAR_RIGHT_DOOR, rearRightDoorOpen)
        )
    }

    private fun VehicleState.toUiState() = VehicleUiState(
        gatewayStatus = "Connected",
        gatewayMessage = "Live vehicle data",
        speed = speed,
        battery = battery,
        ignitionOn = ignitionOn,
        headlightsOn = headlightsOn,
        headlightBeam = headlightBeam,
        hvacOn = hvacOn,
        temperature = temperature,
        gear = gear,
        frontLeftDoorOpen = frontLeftDoorOpen,
        frontRightDoorOpen = frontRightDoorOpen,
        rearLeftDoorOpen = rearLeftDoorOpen,
        rearRightDoorOpen = rearRightDoorOpen
    )
}
