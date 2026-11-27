
package com.example.autocoreividashboard.vhal

data class VhalPropertyEvent(
    val propertyId: Int,
    val areaId: Int,
    val value: Any,
    val timestampMillis: Long = System.currentTimeMillis()
)
