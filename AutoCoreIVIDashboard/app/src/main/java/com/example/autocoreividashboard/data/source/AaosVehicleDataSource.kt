package com.example.autocoreividashboard.data.source

import android.content.Context
import android.util.Log
import com.example.autocoreividashboard.aaos.AaosVehiclePropertyReader
import com.example.autocoreividashboard.aaos.AutoCoreCarServiceManager
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore
import com.example.autocoreividashboard.data.VehicleUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AaosVehicleDataSource(
    context: Context
) : VehicleDataSource {

    override val sourceType = VehicleDataSourceType.AAOS

    private val carServiceManager =
        AutoCoreCarServiceManager(context.applicationContext)

    private val propertyManager =
        VehiclePropertyStore.manager

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _vehicleState =
        MutableStateFlow(VehicleUiState())

    override val vehicleState: StateFlow<VehicleUiState> =
        _vehicleState.asStateFlow()

    override val vehicleProperties =
        propertyManager.observeProperties()

    private var propertyReader: AaosVehiclePropertyReader? = null

    private var connectionStateJob: Job? = null
    private var carPropertyManagerJob: Job? = null
    private var vehiclePropertiesJob: Job? = null

    private var isConnected = false

    override fun connect() {
        if (isConnected) {
            return
        }

        isConnected = true

        observeCarService()
        observeVehicleProperties()

        carServiceManager.connect()
    }

    private fun observeCarService() {
        connectionStateJob?.cancel()

        connectionStateJob = scope.launch {
            carServiceManager.connectionState.collect { state ->
                _vehicleState.value = _vehicleState.value.copy(
                    gatewayStatus = when (state) {
                        AutoCoreCarServiceManager.ConnectionState.DISCONNECTED ->
                            "Disconnected"

                        AutoCoreCarServiceManager.ConnectionState.CONNECTING ->
                            "Connecting"

                        AutoCoreCarServiceManager.ConnectionState.CONNECTED ->
                            "Connected"
                    },
                    gatewayMessage = when (state) {
                        AutoCoreCarServiceManager.ConnectionState.DISCONNECTED ->
                            "AAOS Car Service disconnected"

                        AutoCoreCarServiceManager.ConnectionState.CONNECTING ->
                            "Connecting to AAOS Car Service"

                        AutoCoreCarServiceManager.ConnectionState.CONNECTED ->
                            "Receiving AAOS vehicle properties"
                    }
                )
            }
        }

        carPropertyManagerJob?.cancel()

        carPropertyManagerJob = scope.launch {
            carServiceManager.carPropertyManager.collect { manager ->
                if (manager == null) {
                    propertyReader?.unsubscribeFromVehicleProperties()
                    propertyReader = null
                    return@collect
                }

                if (propertyReader != null) {
                    return@collect
                }

                val reader = AaosVehiclePropertyReader(
                    propertyManager = manager,
                    onPropertyUpdated = { property ->
                        Log.d(
                            "AaosVehicleDataSource",
                            "Reader callback: ${property.key}=${property.value} " +
                                    "(${property.value?.javaClass?.simpleName})"
                        )
                        propertyManager.setProperty(property)
                    }
                )

                propertyReader = reader

                reader.apply {
                    logSupportedProperties()
                    readInitialVehicleProperties()
                    subscribeToVehicleProperties()
                }
            }
        }
    }

    private fun observeVehicleProperties() {
        vehiclePropertiesJob?.cancel()

        vehiclePropertiesJob = scope.launch {
            Log.d("AaosVehicleDataSource", "Starting vehicle property observation")

            vehicleProperties.collect { properties ->
                Log.d(
                    "AaosVehicleDataSource",
                    "Received properties: " + properties.entries.joinToString { (key, property) ->
                        "$key=${property.value} (${property.value?.javaClass?.simpleName})"
                    }
                )

                val currentState = _vehicleState.value

                _vehicleState.value = currentState.copy(

                    ignitionOn = properties[
                        VehiclePropertyKey.IGNITION_STATE
                    ]?.value as? Boolean ?: currentState.ignitionOn,

                    gear = properties[
                        VehiclePropertyKey.GEAR_SELECTION
                    ]?.value as? String ?: currentState.gear,

                    parkingBrakeOn = properties[
                        VehiclePropertyKey.PARKING_BRAKE_ON
                    ]?.value as? Boolean ?: currentState.parkingBrakeOn,

                    speed = (properties[
                        VehiclePropertyKey.SPEED]?.value as? Number)
                        ?.toInt() ?: currentState.speed,

                    battery = (properties[VehiclePropertyKey.BATTERY_LEVEL]?.value as? Number)
                        ?.toInt() ?: currentState.battery,

                    temperature = (properties[VehiclePropertyKey.CABIN_TEMPERATURE]?.value as? Number)
                        ?.toInt() ?: currentState.temperature,

                    hvacOn = properties[
                        VehiclePropertyKey.HVAC_STATE
                    ]?.value as? Boolean ?: currentState.hvacOn,
                )

                Log.d(
                    "AaosVehicleDataSource",
                    properties.entries.joinToString { (key, property) ->
                        "$key=${property.value} (${property.value?.javaClass?.simpleName})"
                    }
                )
            }
        }
    }

    override fun disconnect() {
        if (!isConnected) {
            return
        }

        isConnected = false

        // Stop property subscriptions.
        propertyReader?.unsubscribeFromVehicleProperties()
        propertyReader = null

        // Stop observation collectors.
        connectionStateJob?.cancel()
        connectionStateJob = null

        carPropertyManagerJob?.cancel()
        carPropertyManagerJob = null

        vehiclePropertiesJob?.cancel()
        vehiclePropertiesJob = null


        // Disconnect from Android Car Service.
        carServiceManager.disconnect()

        _vehicleState.value = _vehicleState.value.copy(
            gatewayStatus = "Disconnected",
            gatewayMessage = "AAOS Car Service disconnected"
        )
    }
}
