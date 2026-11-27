package com.example.autocoreividashboard.vhal

/**
 * Standard Android Automotive vehicle area identifiers.
 * Reference: Android VehicleAreaDoor.aidl
 */
enum class VhalDoorAreaId(val id: Int) {
    ROW_1_LEFT(0x00000001),
    ROW_1_RIGHT(0x00000004),
    ROW_2_LEFT(0x00000010),
    ROW_2_RIGHT(0x00000040)
}
