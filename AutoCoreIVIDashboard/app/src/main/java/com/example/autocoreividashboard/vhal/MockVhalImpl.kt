package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyStore

class MockVhalImpl : MockVhal {

    private val properties =
        mutableMapOf<Pair<Int, Int>, VhalPropertyValue>()

    override fun setProperty(property: VehicleProperty<*>) {
        val converted = VhalValueConverter.convert(property)

        validateProperty(converted)
        writeNativeIfSupported(converted)

        synchronized(properties) {
            properties[Pair(converted.propertyId, converted.areaId)] = converted
        }
    }

    override fun setPropertyValue(
        propertyId: Int,
        areaId: Int,
        value: Any
    ) {
        val propertyIdEnum = VhalPropertyId.values()
            .find { it.id == propertyId }
            ?: throw IllegalArgumentException(
                "Unknown property ID: $propertyId"
            )

        val config = VhalPropertyConfigs.getConfig(propertyIdEnum)
            ?: throw IllegalArgumentException(
                "Property is not configured: $propertyId"
            )

        require(config.accessMode != VhalAccessMode.READ) {
            "Property is read-only: $propertyId"
        }

        require(areaId in config.areaIds) {
            "Invalid area ID $areaId for property $propertyId"
        }

        val property = VhalPropertyValue(
            propertyId = propertyId,
            areaId = areaId,
            value = value
        )

        validateProperty(property)

        val vehicleProperty =
            VhalValueConverter.convertToVehicleProperty(property)

        // Keep the central Kotlin property manager as the source of truth.
        VehiclePropertyStore.manager.setProperty(vehicleProperty)

        writeNativeIfSupported(property)

        synchronized(properties) {
            properties[Pair(propertyId, areaId)] = property
        }
    }

    override fun getProperty(
        propertyId: Int,
        areaId: Int
    ): VhalPropertyValue? {
        val propertyIdEnum = VhalPropertyId.values()
            .find { it.id == propertyId }
            ?: throw IllegalArgumentException(
                "Unknown property ID: $propertyId"
            )

        val config = VhalPropertyConfigs.getConfig(propertyIdEnum)
            ?: throw IllegalArgumentException(
                "Property is not configured: $propertyId"
            )

        if (config.accessMode == VhalAccessMode.WRITE) {
            throw IllegalArgumentException(
                "Property is write-only: $propertyId"
            )
        }

        require(areaId in config.areaIds) {
            "Invalid area ID $areaId for property $propertyId"
        }

        val nativeValue = readNativeIfSupported(propertyId, areaId)

        return synchronized(properties) {
            val key = Pair(propertyId, areaId)
            val cached = properties[key]

            if (nativeValue != null) {
                VhalPropertyValue(
                    propertyId = propertyId,
                    areaId = areaId,
                    value = nativeValue
                ).also { properties[key] = it }
            } else {
                cached
            }
        }
    }

    override fun getAllProperties(): List<VhalPropertyValue> {
        return synchronized(properties) {
            properties.values.toList()
        }
    }

    private fun writeNativeIfSupported(property: VhalPropertyValue) {
        if (!isNativeProperty(property.propertyId, property.areaId)) {
            return
        }

        val value = property.value as? Float
            ?: throw IllegalArgumentException(
                "Native property must be Float: ${property.propertyId}"
            )

        check(
            NativeVhalBridge.nativeSetPropertyValue(
                property.propertyId,
                property.areaId,
                value
            )
        ) {
            "Native write failed for property=${property.propertyId}, " +
                    "areaId=${property.areaId}"
        }
    }

    private fun readNativeIfSupported(
        propertyId: Int,
        areaId: Int
    ): Float? {
        if (!isNativeProperty(propertyId, areaId)) {
            return null
        }

        val value = NativeVhalBridge.nativeGetPropertyValue(propertyId, areaId)

        // Native getter uses -1.0f to indicate no stored value.
        return value.takeIf { it >= 0.0f }
    }

    private fun isNativeProperty(
        propertyId: Int,
        areaId: Int
    ): Boolean {
        return areaId == VhalAreaId.GLOBAL &&
                (
                        propertyId == VhalPropertyId.PERF_VEHICLE_SPEED.id ||
                                propertyId == VhalPropertyId.EV_BATTERY_LEVEL.id
                        )
    }

    private fun validateProperty(
        property: VhalPropertyValue
    ) {
        val propertyIdEnum = VhalPropertyId.values()
            .find { it.id == property.propertyId }
            ?: throw IllegalArgumentException(
                "Unknown property ID: ${property.propertyId}"
            )

        val config = VhalPropertyConfigs.getConfig(propertyIdEnum)
            ?: throw IllegalArgumentException(
                "Missing configuration: ${property.propertyId}"
            )

        require(property.areaId in config.areaIds) {
            "Invalid area ID: ${property.areaId}"
        }

        val validType = when (config.propertyType) {
            VhalPropertyType.BOOLEAN ->
                property.value is Boolean

            VhalPropertyType.INT32 ->
                property.value is Int

            VhalPropertyType.INT64 ->
                property.value is Long

            VhalPropertyType.FLOAT ->
                property.value is Float

            VhalPropertyType.STRING ->
                property.value is String

            VhalPropertyType.INT32_VEC ->
                property.value is IntArray

            VhalPropertyType.INT64_VEC ->
                property.value is LongArray

            VhalPropertyType.FLOAT_VEC ->
                property.value is FloatArray

            VhalPropertyType.BYTES ->
                property.value is ByteArray
        }

        require(validType) {
            "Invalid data type for property ${property.propertyId}: " +
                    "${property.value::class.simpleName}"
        }

        if (property.value is Number) {
            val value = property.value.toFloat()

            require(
                config.minValue == null ||
                        value >= config.minValue
            ) {
                "Value below minimum: $value"
            }

            require(
                config.maxValue == null ||
                        value <= config.maxValue
            ) {
                "Value above maximum: $value"
            }
        }
    }
}
