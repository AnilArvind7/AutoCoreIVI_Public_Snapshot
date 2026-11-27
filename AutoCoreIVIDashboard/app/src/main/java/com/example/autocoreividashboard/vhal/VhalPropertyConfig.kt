package com.example.autocoreividashboard.vhal

data class VhalPropertyConfig(
    val propertyId: VhalPropertyId,
    val propertyType: VhalPropertyType,
    val accessMode: VhalAccessMode,
    val changeMode: VhalChangeMode,
    val areaIds: List<Int> = listOf(VhalAreaId.GLOBAL),
    val minValue: Float? = null,
    val maxValue: Float? = null
)
