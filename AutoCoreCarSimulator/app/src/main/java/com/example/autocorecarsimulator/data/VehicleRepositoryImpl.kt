package com.example.autocorecarsimulator.data

import com.example.autocorecarsimulator.socket.VehicleGatewayClient
import com.example.autocorecarsimulator.socket.VehicleState
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

class VehicleRepositoryImpl : VehicleRepository {

    private val _vehicleState = MutableStateFlow(VehicleUiState())

    override val vehicleState: StateFlow<VehicleUiState> =
        _vehicleState.asStateFlow()

    private val propertyManager = VehiclePropertyManager()

    override val vehicleProperties =
        propertyManager.observeProperties()

    override fun getProperty(key: VehiclePropertyKey): VehicleProperty<*>? {
        return propertyManager.getProperty(key)
    }

    private val repositoryScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var retryJob: Job? = null
    private var retryAttempt = 0

    @Volatile
    private var shouldReconnect = true

    private val gatewayClient: VehicleGatewayClient by lazy {
        VehicleGatewayClient(
            onConnected = {
                retryAttempt = 0
                retryJob?.cancel()
                retryJob = null

                _vehicleState.value = _vehicleState.value.copy(
                    gateway = GatewayConnectionState(
                        status = "Connected",
                        message = "Publishing vehicle data"
                    )
                )

                // Publish latest state immediately after connection.
                gatewayClient.sendState(
                    _vehicleState.value.vehicleData.toVehicleState()
                )
            },

            onStateReceived = {
                // Simulator normally produces vehicle state.
            },

            onError = { message ->
                if (shouldReconnect) {
                    _vehicleState.value = _vehicleState.value.copy(
                        gateway = GatewayConnectionState(
                            status = "Reconnecting",
                            message = message
                        )
                    )
                    scheduleReconnect()
                }
            },

            onDisconnected = {
                if (shouldReconnect) {
                    _vehicleState.value = _vehicleState.value.copy(
                        gateway = GatewayConnectionState(
                            status = "Reconnecting",
                            message = "Gateway connection closed"
                        )
                    )
                    scheduleReconnect()
                }
            }
        )
    }

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
            gateway = GatewayConnectionState(
                status = "Disconnected",
                message = "Simulator closed"
            )
        )
    }

    override fun updateVehicleData(data: VehicleData) {
        val currentState = _vehicleState.value

        _vehicleState.value = currentState.copy(
            vehicleData = data
        )

        updatePropertyManager(data)

        if (currentState.gateway.status == "Connected") {
            gatewayClient.sendState(data.toVehicleState())
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || retryJob?.isActive == true) return

        val delaySeconds = (2L shl retryAttempt).coerceAtMost(30L)
        retryAttempt++

        retryJob = repositoryScope.launch {
            for (remaining in delaySeconds downTo 1) {
                if (!shouldReconnect) return@launch

                _vehicleState.value = _vehicleState.value.copy(
                    gateway = GatewayConnectionState(
                        status = "Reconnecting",
                        message = "Retrying in ${remaining}s"
                    )
                )

                delay(1000.milliseconds)
            }

            if (shouldReconnect) {
                gatewayClient.connect()
            }
        }
    }

    private fun updatePropertyManager(data: VehicleData) {

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.SPEED, data.speed)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.BATTERY_LEVEL, data.battery)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.IGNITION_STATE, data.ignitionOn)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HEADLIGHT_STATE, data.headlightsOn)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HEADLIGHT_BEAM, data.headlightBeam)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.HVAC_STATE, data.hvacOn)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.CABIN_TEMPERATURE, data.temperature)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.GEAR_SELECTION, data.gear)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.FRONT_LEFT_DOOR, data.frontLeftDoorOpen)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.FRONT_RIGHT_DOOR, data.frontRightDoorOpen)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.REAR_LEFT_DOOR, data.rearLeftDoorOpen)
        )

        propertyManager.setProperty(
            VehicleProperty(VehiclePropertyKey.REAR_RIGHT_DOOR, data.rearRightDoorOpen)
        )
    }

    private fun VehicleData.toVehicleState() = VehicleState(
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
