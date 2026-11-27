package com.example.autocoreividashboard.vhal

import android.util.Log

object NativeVhalTest {

    fun testNativeReads() {
        val vhal = VhalRuntime.mockVhal

        val speed = vhal.getProperty(
            VhalPropertyId.PERF_VEHICLE_SPEED.id,
            VhalAreaId.GLOBAL
        )

        val battery = vhal.getProperty(
            VhalPropertyId.EV_BATTERY_LEVEL.id,
            VhalAreaId.GLOBAL
        )

        Log.d("NativeVhalTest", "Speed = ${speed?.value}")
        Log.d("NativeVhalTest", "Battery = ${battery?.value}")
    }
}
