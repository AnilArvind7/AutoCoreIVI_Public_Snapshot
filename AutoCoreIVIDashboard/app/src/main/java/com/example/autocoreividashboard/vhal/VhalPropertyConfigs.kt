package com.example.autocoreividashboard.vhal

object VhalPropertyConfigs {
    private val configs = listOf(

        // Vehicle speed
        VhalPropertyConfig(
            propertyId = VhalPropertyId.PERF_VEHICLE_SPEED,
            propertyType = VhalPropertyType.FLOAT,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.CONTINUOUS,
            minValue = 0f
        ),

        // EV battery level
        VhalPropertyConfig(
            propertyId = VhalPropertyId.EV_BATTERY_LEVEL,
            propertyType = VhalPropertyType.FLOAT,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE,
            minValue = 0f,
            maxValue = 100f
        ),

        // Ignition state
        VhalPropertyConfig(
            propertyId = VhalPropertyId.IGNITION_STATE,
            propertyType = VhalPropertyType.INT32,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE
        ),

        // Headlights
        VhalPropertyConfig(
            propertyId = VhalPropertyId.HEADLIGHTS_STATE,
            propertyType = VhalPropertyType.INT32,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE
        ),

        // High beam
        VhalPropertyConfig(
            propertyId = VhalPropertyId.HIGH_BEAM_LIGHTS_STATE,
            propertyType = VhalPropertyType.INT32,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE
        ),

        // HVAC power
        VhalPropertyConfig(
            propertyId = VhalPropertyId.HVAC_POWER_ON,
            propertyType = VhalPropertyType.BOOLEAN,
            accessMode = VhalAccessMode.READ_WRITE,
            changeMode = VhalChangeMode.ON_CHANGE,
            areaIds = listOf(VhalHvacAreaId.ROW_1_LEFT.id)
        ),

        // HVAC temperature setpoint
        VhalPropertyConfig(
            propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET,
            propertyType = VhalPropertyType.FLOAT,
            accessMode = VhalAccessMode.READ_WRITE,
            changeMode = VhalChangeMode.ON_CHANGE,
            areaIds = listOf(VhalHvacAreaId.ROW_1_LEFT.id),
            minValue = 16f,
            maxValue = 30f
        ),

        // Parking brake
        VhalPropertyConfig(
            propertyId = VhalPropertyId.PARKING_BRAKE_ON,
            propertyType = VhalPropertyType.BOOLEAN,
            accessMode = VhalAccessMode.READ_WRITE,
            changeMode = VhalChangeMode.ON_CHANGE
        ),

        // Gear selection
        VhalPropertyConfig(
            propertyId = VhalPropertyId.GEAR_SELECTION,
            propertyType = VhalPropertyType.INT32,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE
        ),

        // Door positions - four areas, one property ID
        VhalPropertyConfig(
            propertyId = VhalPropertyId.DOOR_POS,
            propertyType = VhalPropertyType.INT32,
            accessMode = VhalAccessMode.READ,
            changeMode = VhalChangeMode.ON_CHANGE,
            areaIds = listOf(
                VhalDoorAreaId.ROW_1_LEFT.id,
                VhalDoorAreaId.ROW_1_RIGHT.id,
                VhalDoorAreaId.ROW_2_LEFT.id,
                VhalDoorAreaId.ROW_2_RIGHT.id
            ),
            minValue = 0f,
            maxValue = 100f
        )
    )

    fun validateAll(): List<String> {
        return VhalPropertyConfigValidator.validate(configs)
    }

    fun getAll(): List<VhalPropertyConfig> = configs

    fun getConfig(
        propertyId: VhalPropertyId
    ): VhalPropertyConfig? {
        return configs.find { it.propertyId == propertyId }
    }
}
