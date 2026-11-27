
package com.example.autocoreividashboard.vhal

/**
 * HVAC area identifiers based on Android Automotive
 * VehicleAreaSeat definitions.
 *
 * These are separate from door-area identifiers,
 * even where their numeric values are identical.
 */
enum class VhalHvacAreaId(val id: Int) {
    ROW_1_LEFT(0x00000001),
    ROW_1_RIGHT(0x00000004),
    ROW_2_LEFT(0x00000010),
    ROW_2_RIGHT(0x00000040)
}
