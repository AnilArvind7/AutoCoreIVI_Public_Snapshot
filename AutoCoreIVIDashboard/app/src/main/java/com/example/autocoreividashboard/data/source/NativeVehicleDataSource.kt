package com.example.autocoreividashboard.data.source

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore
import com.example.autocoreividashboard.data.VehicleUiState
import com.example.autocoreividashboard.vhal.NativeVhalBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class NativeVehicleDataSource : VehicleDataSource {

    override val sourceType = VehicleDataSourceType.NATIVE

    private val propertyManager = VehiclePropertyStore.manager

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _vehicleState = MutableStateFlow(
        VehicleUiState(
            gatewayStatus = "Disconnected",
            gatewayMessage = "Native property store is stopped"
        )
    )

    override val vehicleState: StateFlow<VehicleUiState> =
        _vehicleState.asStateFlow()

    override val vehicleProperties =
        propertyManager.observeProperties()

    private var pollingJob: Job? = null

    override fun connect() {
        if (pollingJob?.isActive == true) return

        _vehicleState.value = _vehicleState.value.copy(
            gatewayStatus = "Connected",
            gatewayMessage = "Reading native C++ property store"
        )

        pollingJob = scope.launch {
            while (true) {
                readNativeProperties()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun readNativeProperties() {
        val nativeSpeed = NativeVhalBridge.nativeGetPropertyValue(
            PROPERTY_SPEED,
            GLOBAL_AREA_ID
        )

        val nativeBattery = NativeVhalBridge.nativeGetPropertyValue(
            PROPERTY_BATTERY,
            GLOBAL_AREA_ID
        )

        val speedKmh = nativeSpeed
            .takeIf { it >= 0f }
            ?.let { (it * METERS_PER_SECOND_TO_KMH).roundToInt() }

        val batteryPercent = nativeBattery
            .takeIf { it >= 0f }
            ?.roundToInt()

        speedKmh?.let { speed ->
            propertyManager.setProperty(
                VehicleProperty(VehiclePropertyKey.SPEED, speed)
            )
        }

        batteryPercent?.let { battery ->
            propertyManager.setProperty(
                VehicleProperty(VehiclePropertyKey.BATTERY_LEVEL, battery)
            )
        }

        _vehicleState.value = _vehicleState.value.copy(
            gatewayStatus = "Connected",
            gatewayMessage = "Native C++ property store active",
            speed = speedKmh ?: _vehicleState.value.speed,
            battery = batteryPercent ?: _vehicleState.value.battery
        )
    }

    override fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null

        _vehicleState.value = _vehicleState.value.copy(
            gatewayStatus = "Disconnected",
            gatewayMessage = "Native property store stopped"
        )
    }

    private companion object {
        const val PROPERTY_SPEED = 0x11600207
        const val PROPERTY_BATTERY = 0x11600309
        const val GLOBAL_AREA_ID = 0

        const val POLL_INTERVAL_MS = 500L
        const val METERS_PER_SECOND_TO_KMH = 3.6f
    }
}
