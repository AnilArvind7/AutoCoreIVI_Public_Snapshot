package com.example.autocoreividashboard.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.example.autocoreividashboard.IVehiclePropertyCallback
import com.example.autocoreividashboard.IVehiclePropertyService
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.example.autocoreividashboard.vhal.VhalRuntime

class VehiclePropertyService : Service() {

    private companion object {
        private const val TAG = "VehiclePropertyService"
        private val KEY_MAP: Map<String, VehiclePropertyKey> =
            VehiclePropertyKey.entries.associateBy { it.name }
    }

    private val propertyManager = VehiclePropertyStore.manager

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val callbacks =
        RemoteCallbackList<IVehiclePropertyCallback>()

    private val callbackLock = Any()

    private val binder = object : IVehiclePropertyService.Stub() {

        override fun getProperty(propertyName: String): String {
            // Optimization: O(1) map lookup replaces runCatching { VehiclePropertyKey.valueOf(...) }
            val key = KEY_MAP[propertyName] ?: return ""

            return propertyManager
                .getProperty(key)
                ?.value
                ?.toString() ?: ""
        }

        override fun setProperty(
            propertyName: String,
            value: String
        ) {
            // Optimization: O(1) map lookup avoids exception allocation overhead on invalid strings
            val key = KEY_MAP[propertyName] ?: return

            val parsedValue: Any = when (key) {
                VehiclePropertyKey.SPEED,
                VehiclePropertyKey.BATTERY_LEVEL,
                VehiclePropertyKey.CABIN_TEMPERATURE ->
                    value.toIntOrNull() ?: return

                VehiclePropertyKey.IGNITION_STATE,
                VehiclePropertyKey.HEADLIGHT_STATE,
                VehiclePropertyKey.PARKING_BRAKE_ON,
                VehiclePropertyKey.HVAC_STATE,
                VehiclePropertyKey.FRONT_LEFT_DOOR,
                VehiclePropertyKey.FRONT_RIGHT_DOOR,
                VehiclePropertyKey.REAR_LEFT_DOOR,
                VehiclePropertyKey.REAR_RIGHT_DOOR ->
                    value.toBooleanStrictOrNull() ?: return

                VehiclePropertyKey.HEADLIGHT_BEAM,
                VehiclePropertyKey.GEAR_SELECTION -> value
            }

            VhalRuntime.propertyAdapter.setProperty(key, parsedValue)
        }

        override fun registerCallback(callback: IVehiclePropertyCallback?) {
            if (callback != null) {
                synchronized(callbackLock) {
                    callbacks.register(callback)
                }
            }
        }

        override fun unregisterCallback(callback: IVehiclePropertyCallback?) {
            if (callback != null) {
                synchronized(callbackLock) {
                    callbacks.unregister(callback)
                }
            }
        }
    }

    private fun broadcastPropertyChange(
        propertyName: String,
        value: String
    ) {
        // Optimization: Exit immediately if no clients are listening to avoid unnecessary broadcast setup
        if (callbacks.registeredCallbackCount == 0) return

        val startTimeNanos = if (Log.isLoggable(TAG, Log.DEBUG)) System.nanoTime() else 0L

        synchronized(callbackLock) {
            val count = callbacks.beginBroadcast()

            try {
                for (i in 0 until count) {
                    try {
                        callbacks.getBroadcastItem(i)
                            .onPropertyChanged(propertyName, value)
                    } catch (exception: RemoteException) {
                        Log.e(
                            TAG,
                            "Callback failed for $propertyName",
                            exception
                        )
                    }
                }
            } finally {
                callbacks.finishBroadcast()
            }
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            val elapsedMicros = (System.nanoTime() - startTimeNanos) / 1000
            Log.d(TAG, "Broadcast $propertyName=$value to ${callbacks.registeredCallbackCount} client(s) in ${elapsedMicros}µs")
        }
    }

    private fun observePropertyChanges() {
        serviceScope.launch {
            // Lightweight map tracking property values only, preventing retention of full object maps
            val previousValues = HashMap<VehiclePropertyKey, Any?>(VehiclePropertyKey.entries.size)

            // Seed initial property values so initial state is not redundantly broadcasted
            propertyManager.observeProperties().value.forEach { (key, property) ->
                previousValues[key] = property.value
            }

            propertyManager.observeProperties().collect { properties ->
                // Fast path: if no listeners registered, keep cache updated without string allocations or broadcasts
                if (callbacks.registeredCallbackCount == 0) {
                    properties.forEach { (key, property) ->
                        previousValues[key] = property.value
                    }
                    return@collect
                }

                properties.forEach { (key, property) ->
                    val newValue = property.value
                    val previousValue = previousValues[key]

                    if (previousValue != newValue) {
                        previousValues[key] = newValue
                        // String conversion happens ONLY when a value change is confirmed
                        broadcastPropertyChange(
                            key.name,
                            newValue.toString()
                        )
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        observePropertyChanges()
    }

    override fun onDestroy() {
        // Optimization: Cancel coroutine scope to halt background collection
        serviceScope.cancel()
        // Optimization: Kill RemoteCallbackList to release binder references and prevent memory leaks
        callbacks.kill()
        super.onDestroy()
    }
}
