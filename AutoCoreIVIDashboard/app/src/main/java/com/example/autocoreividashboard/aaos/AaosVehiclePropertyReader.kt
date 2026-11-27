package com.example.autocoreividashboard.aaos

import android.car.VehicleAreaType
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey
import kotlin.math.roundToInt

class AaosVehiclePropertyReader(
    private val propertyManager: CarPropertyManager,
    private val onPropertyUpdated: (VehicleProperty<*>) -> Unit
) {

    companion object {
        private const val TAG = "AutoCoreAAOS"
        private const val GLOBAL_AREA_ID = 0
    }

    private val subscribedPropertyIds = mutableSetOf<Int>()
    private val callbackLock = Any()
    private var acceptingCallbacks = false
    private var batteryLevelWh: Float? = null
    private var batteryCapacityWh: Float? = null

    /**
     * Logs properties supported by AAOS Car Service.
     */
    fun logSupportedProperties() {
        Log.i(TAG, "PROPERTY DISCOVERY STARTED")

        try {
            val supportedProperties = propertyManager.propertyList
            val hvacPropertyIds = listOf(
                VehiclePropertyIds.HVAC_POWER_ON,
                VehiclePropertyIds.HVAC_TEMPERATURE_SET
            )
            val doorPropertyIds = listOf(
                VehiclePropertyIds.DOOR_POS
            )
            val lightingPropertyIds = listOf(
                VehiclePropertyIds.HEADLIGHTS_STATE,
                VehiclePropertyIds.HIGH_BEAM_LIGHTS_STATE
            )
            val turnSignalPropertyIds = listOf(
                VehiclePropertyIds.TURN_SIGNAL_STATE,
                VehiclePropertyIds.TURN_SIGNAL_SWITCH,
                VehiclePropertyIds.TURN_SIGNAL_LIGHT_STATE
            )

            doorPropertyIds.forEach { propertyId ->
                val config = supportedProperties.firstOrNull {
                    it.propertyId == propertyId
                }

                Log.i(
                    TAG,
                    "DOOR AVAILABILITY | " +
                            "Name=${VehiclePropertyIds.toString(propertyId)} | " +
                            "Available=${config != null} | " +
                            "Areas=${config?.areaIds?.contentToString()} | " +
                            "Access=${config?.access}"
                )
            }

            hvacPropertyIds.forEach { propertyId ->

                val config = supportedProperties.firstOrNull {
                    it.propertyId == propertyId
                }

                Log.i(
                    TAG,
                    "HVAC AVAILABILITY | " +
                            "Name=${VehiclePropertyIds.toString(propertyId)} | " +
                            "Available=${config != null} | " +
                            "Areas=${config?.areaIds?.contentToString()} | " +
                            "Access=${config?.access}"
                )
            }

            lightingPropertyIds.forEach { propertyId ->
                val config = supportedProperties.firstOrNull {
                    it.propertyId == propertyId
                }

                Log.i(
                    TAG,
                    "LIGHTING AVAILABILITY | " +
                            "Name=${VehiclePropertyIds.toString(propertyId)} | " +
                            "Available=${config != null} | " +
                            "Areas=${config?.areaIds?.contentToString()} | " +
                            "Access=${config?.access}"
                )
            }

            turnSignalPropertyIds.forEach { propertyId ->
                val config = supportedProperties.firstOrNull {
                    it.propertyId == propertyId
                }

                Log.i(
                    TAG,
                    "TURN SIGNAL AVAILABILITY | " +
                            "Name=${VehiclePropertyIds.toString(propertyId)} | " +
                            "Available=${config != null} | " +
                            "Areas=${config?.areaIds?.contentToString()} | " +
                            "Access=${config?.access}"
                )
            }

            supportedProperties.forEach { config ->
                Log.i(
                    TAG,
                    "Property ID=${config.propertyId}, " +
                            "Access=${config.access}, " +
                            "ChangeMode=${config.changeMode}, " +
                            "Areas=${config.areaIds.contentToString()}"
                )

                Log.i(
                    TAG,
                    "PROPERTY DISCOVERY | " +
                            "Name=${VehiclePropertyIds.toString(config.propertyId)} | " +
                            "ID=${config.propertyId} | " +
                            "Access=${config.access} | " +
                            "ChangeMode=${config.changeMode} | " +
                            "Areas=${config.areaIds}"
                )
            }

            Log.i(
                TAG,
                "Total supported properties: ${supportedProperties.size}"
            )

        } catch (exception: Exception) {
            Log.e(TAG, "Failed to discover vehicle properties", exception)
        }
    }

    /**
     * Reads initial values from CarPropertyManager.
     */
    fun readInitialVehicleProperties() {
        Log.i(TAG, "INITIAL VEHICLE PROPERTY READ STARTED")

        readIntProperty(VehiclePropertyIds.IGNITION_STATE)
        readIntProperty(VehiclePropertyIds.GEAR_SELECTION)
        readBooleanProperty(VehiclePropertyIds.PARKING_BRAKE_ON)
        readFloatProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED)
        readFloatProperty(VehiclePropertyIds.EV_BATTERY_LEVEL)
        readFloatProperty(VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY)
        readBooleanPropertyForArea(VehiclePropertyIds.HVAC_POWER_ON)
        readFloatPropertyForArea(VehiclePropertyIds.HVAC_TEMPERATURE_SET)

        Log.i(
            TAG,
            "PERF_VEHICLE_SPEED supported: ${
                propertyManager.isPropertyAvailable(
                    VehiclePropertyIds.PERF_VEHICLE_SPEED,
                    GLOBAL_AREA_ID
                )
            }"
        )
    }

    private fun readIntProperty(propertyId: Int) {
        try {
            val value = propertyManager.getProperty<Int>(
                propertyId,
                GLOBAL_AREA_ID
            )

            if (value != null) {
                updateAutoCoreProperty(
                    propertyId = propertyId,
                    rawValue = value.value,
                    areaId = value.areaId,
                    timestampNanos = value.timestamp
                )
            }

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to read integer property: $propertyId",
                exception
            )
        }
    }

    private fun readBooleanProperty(propertyId: Int) {
        try {
            val value = propertyManager.getProperty<Boolean>(
                propertyId,
                GLOBAL_AREA_ID
            )

            if (value != null) {
                updateAutoCoreProperty(
                    propertyId = propertyId,
                    rawValue = value.value,
                    areaId = value.areaId,
                    timestampNanos = value.timestamp
                )
            }

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Failed to read Boolean property: $propertyId",
                exception
            )
        }
    }

    private fun readFloatProperty(propertyId: Int) {
        try {
            val value = propertyManager.getProperty<Float>(
                propertyId,
                VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
            )

            value?.value?.let {
                updateAutoCoreProperty(
                    propertyId = propertyId,
                    rawValue = it,
                    areaId = value.areaId,
                    timestampNanos = value.timestamp
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read property: $propertyId", e)
        }
    }

    private fun readBooleanPropertyForArea(propertyId: Int) {
        try {
            val areaId = getPrimaryAreaId(propertyId) ?: return

            val value = propertyManager.getProperty<Boolean>(
                propertyId,
                areaId
            )

            value?.let {
                updateAutoCoreProperty(
                    propertyId = propertyId,
                    rawValue = it.value,
                    areaId = it.areaId,
                    timestampNanos = it.timestamp
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to read HVAC Boolean property: $propertyId", exception)
        }
    }

    private fun readFloatPropertyForArea(propertyId: Int) {
        try {
            val areaId = getPrimaryAreaId(propertyId) ?: return

            val value = propertyManager.getProperty<Float>(
                propertyId,
                areaId
            )

            value?.let {
                updateAutoCoreProperty(
                    propertyId = propertyId,
                    rawValue = it.value,
                    areaId = it.areaId,
                    timestampNanos = it.timestamp
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to read HVAC temperature: $propertyId", exception)
        }
    }

    /**
     * Receives live property updates from AAOS.
     */
    private val propertyCallback =
        object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                synchronized(callbackLock) {
                    if (!acceptingCallbacks) {
                        Log.d(
                            TAG,
                            "Ignoring property callback after unsubscribe: ${value.propertyId}"
                        )
                        return
                    }

                    Log.i(
                        TAG,
                        "LIVE PROPERTY UPDATE | " +
                                "ID=${value.propertyId} | " +
                                "Area=${value.areaId} | " +
                                "Value=${value.value} | " +
                                "Timestamp=${value.timestamp}"
                    )

                    updateAutoCoreProperty(
                        propertyId = value.propertyId,
                        rawValue = value.value,
                        areaId = value.areaId,
                        timestampNanos = value.timestamp
                    )
                }
            }

            override fun onErrorEvent(
                propertyId: Int,
                areaId: Int
            ) {
                Log.e(
                    TAG,
                    "Vehicle property error | " +
                            "ID=$propertyId | Area=$areaId"
                )
            }
        }

    /**
     * Subscribes to live vehicle property updates.
     */
    fun subscribeToVehicleProperties() {
        Log.i(TAG, "STARTING VEHICLE PROPERTY SUBSCRIPTIONS")
        synchronized(callbackLock) {
            acceptingCallbacks = true
        }

        val propertyIds = listOf(
            VehiclePropertyIds.IGNITION_STATE,
            VehiclePropertyIds.GEAR_SELECTION,
            VehiclePropertyIds.PARKING_BRAKE_ON,
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY,
            VehiclePropertyIds.HVAC_POWER_ON,
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
        )

        propertyIds.forEach { propertyId ->
            try {
                val registered = propertyManager.registerCallback(
                    propertyCallback,
                    propertyId,
                    0f
                )

                if (registered) {
                    subscribedPropertyIds.add(propertyId)

                    Log.i(
                        TAG,
                        "Subscribed successfully: $propertyId"
                    )
                } else {
                    Log.w(
                        TAG,
                        "Subscription rejected: $propertyId"
                    )
                }

            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to subscribe: $propertyId",
                    exception
                )
            }
        }
    }

    /**
     * Unsubscribes from registered properties.
     */
    fun unsubscribeFromVehicleProperties() {
        Log.i(TAG, "STOPPING VEHICLE PROPERTY SUBSCRIPTIONS")
        synchronized(callbackLock) {
            acceptingCallbacks = false
        }

        subscribedPropertyIds.toList().forEach { propertyId ->
            try {
                propertyManager.unregisterCallback(
                    propertyCallback,
                    propertyId
                )

                Log.i(
                    TAG,
                    "Unsubscribed successfully: $propertyId"
                )

            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to unsubscribe: $propertyId",
                    exception
                )
            }
        }

        subscribedPropertyIds.clear()
    }

    /**
     * Converts AAOS values into AutoCore vehicle properties.
     *
     * Each supported property is emitted independently
     * through the onPropertyUpdated callback.
     */
    private fun updateAutoCoreProperty(
        propertyId: Int,
        rawValue: Any?,
        areaId: Int,
        timestampNanos: Long
    ) {
        if (rawValue == null) {
            Log.w(TAG, "Ignoring null property: $propertyId")
            return
        }

        val timestampMillis = timestampNanos / 1_000_000L

        when (propertyId) {

            // -----------------------------------------
            // IGNITION STATE
            // -----------------------------------------

            VehiclePropertyIds.IGNITION_STATE -> {
                val ignitionState = rawValue as? Int ?: return

                val isIgnitionOn = when (ignitionState) {
                    VehicleIgnitionState.ON,
                    VehicleIgnitionState.START -> true

                    VehicleIgnitionState.UNDEFINED,
                    VehicleIgnitionState.LOCK,
                    VehicleIgnitionState.OFF,
                    VehicleIgnitionState.ACC -> false

                    else -> {
                        Log.w(
                            TAG,
                            "Unknown ignition state: $ignitionState"
                        )
                        return
                    }
                }

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.IGNITION_STATE,
                        value = isIgnitionOn,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )

                Log.i(
                    TAG,
                    "Ignition property emitted: $isIgnitionOn"
                )
            }

            // -----------------------------------------
            // GEAR SELECTION
            // -----------------------------------------

            VehiclePropertyIds.GEAR_SELECTION -> {
                val gearValue = rawValue as? Int ?: return
                val gearLabel = convertGearSelection(gearValue)

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.GEAR_SELECTION,
                        value = gearLabel,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )

                Log.i(
                    TAG,
                    "Gear property emitted: $gearLabel"
                )
            }

            // -----------------------------------------
            // PARKING BRAKE
            // -----------------------------------------

            VehiclePropertyIds.PARKING_BRAKE_ON -> {
                val parkingBrake = rawValue as? Boolean ?: return

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.PARKING_BRAKE_ON,
                        value = parkingBrake,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )

                Log.i(
                    TAG,
                    "Parking brake property emitted: $parkingBrake"
                )
            }

            // -----------------------------------------
            // VEHICLE SPEED
            // -----------------------------------------

            VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                val speedMps = rawValue as? Float ?: return

                val speedKmh = (speedMps * 3.6f).roundToInt()

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.SPEED,
                        value = speedKmh,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )
            }

            VehiclePropertyIds.EV_BATTERY_LEVEL -> {
                batteryLevelWh = rawValue as? Float
                emitBatteryPercentage(areaId, timestampMillis)
            }

            VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY -> {
                batteryCapacityWh = rawValue as? Float
                emitBatteryPercentage(areaId, timestampMillis)
            }

            // -----------------------------------------
            // HVAC
            // -----------------------------------------

            VehiclePropertyIds.HVAC_POWER_ON -> {
                val hvacOn = rawValue as? Boolean ?: return

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.HVAC_STATE,
                        value = hvacOn,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )

                Log.i(TAG, "HVAC power emitted: $hvacOn")
            }

            VehiclePropertyIds.HVAC_TEMPERATURE_SET -> {
                val temperatureCelsius = rawValue as? Float ?: return

                val temperature = temperatureCelsius.roundToInt()

                onPropertyUpdated(
                    VehicleProperty(
                        key = VehiclePropertyKey.CABIN_TEMPERATURE,
                        value = temperature,
                        areaId = areaId,
                        timestampMillis = timestampMillis
                    )
                )

                Log.i(TAG, "HVAC temperature emitted: $temperature°C")
            }

            else -> {
                Log.w(
                    TAG,
                    "Unsupported property mapping: $propertyId"
                )
            }
        }
    }

    /**
     * Converts AAOS gear values into dashboard labels.
     */
    private fun convertGearSelection(gearValue: Int): String {
        return when (gearValue) {
            VehicleGear.GEAR_PARK -> "P"
            VehicleGear.GEAR_REVERSE -> "R"
            VehicleGear.GEAR_NEUTRAL -> "N"
            VehicleGear.GEAR_DRIVE -> "D"

            VehicleGear.GEAR_UNKNOWN -> "UNKNOWN"

            else -> {
                Log.w(TAG, "Unmapped gear value: $gearValue")
                "UNKNOWN"
            }
        }
    }

    private fun emitBatteryPercentage(
        areaId: Int,
        timestampMillis: Long
    ) {
        val level = batteryLevelWh ?: return
        val capacity = batteryCapacityWh ?: return

        if (capacity <= 0f) return

        val percentage = ((level / capacity) * 100f)
            .roundToInt()
            .coerceIn(0, 100)

        Log.i(TAG, "Battery percentage calculated: $percentage%")

        onPropertyUpdated(
            VehicleProperty(
                key = VehiclePropertyKey.BATTERY_LEVEL,
                value = percentage,
                areaId = areaId,
                timestampMillis = timestampMillis
            )
        )
    }

    private fun getPrimaryAreaId(propertyId: Int): Int? {
        val config = propertyManager.propertyList
            .firstOrNull { it.propertyId == propertyId }

        val areaIds = config?.areaIds ?: return null
        if (areaIds.isEmpty()) {
            Log.e(TAG, "No supported areas for property: $propertyId")
            return null
        }
        val areaId = areaIds.first()

        Log.i(
            TAG,
            "HVAC area selected | Property=$propertyId | " +
                    "Area=$areaId | Supported=${areaIds.contentToString()}"
        )
        return areaId
    }
}
