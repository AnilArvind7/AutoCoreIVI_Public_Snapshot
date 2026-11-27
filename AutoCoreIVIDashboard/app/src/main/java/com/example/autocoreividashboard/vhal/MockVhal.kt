package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehicleProperty

interface MockVhal {

    // Internal vehicle-state ingestion
    fun setProperty(property: VehicleProperty<*>)

    // External VHAL write operation
    fun setPropertyValue(
        propertyId: Int,
        areaId: Int,
        value: Any
    )

    // Read a vehicle property
    fun getProperty(
        propertyId: Int,
        areaId: Int
    ): VhalPropertyValue?

    fun getAllProperties(): List<VhalPropertyValue>
}
