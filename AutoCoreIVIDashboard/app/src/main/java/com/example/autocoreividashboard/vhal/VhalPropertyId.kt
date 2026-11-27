
package com.example.autocoreividashboard.vhal

import android.car.VehiclePropertyIds

/**
 * Standard Android Automotive VHAL property IDs.
 *
 * Reference: Android 16 AOSP VehicleProperty.aidl
 *
 * These are simulation constants. They do not provide
 * access to the platform VHAL by themselves.
 */
enum class VhalPropertyId(val id: Int) {

    // Vehicle performance
    PERF_VEHICLE_SPEED(0x11600207),

    // Electric vehicle battery
    EV_BATTERY_LEVEL(0x11600309),

    // Ignition
    IGNITION_STATE(0x11400409),
    PARKING_BRAKE_ON(VehiclePropertyIds.PARKING_BRAKE_ON),

    // Exterior lighting
    HEADLIGHTS_STATE(0x11400E00),
    HIGH_BEAM_LIGHTS_STATE(0x11400E01),

    // HVAC
    HVAC_POWER_ON(0x15200510),
    HVAC_TEMPERATURE_SET(0x15600503),
    HVAC_TEMPERATURE_CURRENT(0x15600502),

    // Transmission
    GEAR_SELECTION(0x11400400),

    // Door positions
    DOOR_POS(0x16400B00)
}
