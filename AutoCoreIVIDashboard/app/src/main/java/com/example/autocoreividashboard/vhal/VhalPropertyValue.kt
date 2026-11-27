
package com.example.autocoreividashboard.vhal

data class VhalPropertyValue(
    val propertyId: Int,
    val areaId: Int,
    val value: Any,
    val timestampMillis: Long = System.currentTimeMillis()
)
