package com.example.autocoreividashboard.aaos

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoCoreCarServiceManager(context: Context) {

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    companion object {
        private const val TAG = "AutoCoreCarService"
    }

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private val _connectionState =
        MutableStateFlow(ConnectionState.DISCONNECTED)

    val connectionState: StateFlow<ConnectionState> =
        _connectionState.asStateFlow()

    private var car: Car? = null

    private val _carPropertyManager =
        MutableStateFlow<CarPropertyManager?>(null)

    val carPropertyManager: StateFlow<CarPropertyManager?> =
        _carPropertyManager.asStateFlow()

    private var connectionGeneration = 0

    fun connect() {
        if (car != null) {
            Log.d(TAG, "Car instance already exists. Skipping connect.")
            return
        }

        val generation = ++connectionGeneration

        _connectionState.value = ConnectionState.CONNECTING
        Log.i(TAG, "Connecting to Android Car Service")

        try {
            car = Car.createCar(
                appContext,
                handler,
                Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
                Car.CarServiceLifecycleListener { carInstance, ready ->

                    // Ignore callbacks belonging to an old connection.
                    if (generation != connectionGeneration) {
                        Log.w(TAG, "Ignoring stale Car Service callback")
                        return@CarServiceLifecycleListener
                    }

                    if (ready) {
                        Log.i(TAG, "Car Service is ready")

                        car = carInstance

                        try {
                            val manager =
                                carInstance.getCarManager(
                                    CarPropertyManager::class.java
                                )

                            if (manager != null) {
                                _carPropertyManager.value = manager
                                _connectionState.value =
                                    ConnectionState.CONNECTED

                                Log.i(TAG, "CarPropertyManager acquired")
                            } else {
                                _carPropertyManager.value = null
                                _connectionState.value =
                                    ConnectionState.DISCONNECTED

                                Log.e(TAG, "CarPropertyManager unavailable")
                            }

                        } catch (exception: Exception) {
                            _carPropertyManager.value = null
                            _connectionState.value =
                                ConnectionState.DISCONNECTED

                            Log.e(
                                TAG,
                                "Failed to acquire CarPropertyManager",
                                exception
                            )
                        }

                    } else {
                        Log.w(TAG, "Car Service disconnected")

                        // Retain the Car instance so its lifecycle
                        // listener can report when service returns.
                        car = carInstance

                        _carPropertyManager.value = null
                        _connectionState.value =
                            ConnectionState.DISCONNECTED
                    }
                }
            )

            Log.d(TAG, "Car.createCar() invoked")

        } catch (exception: Exception) {
            car = null
            _carPropertyManager.value = null
            _connectionState.value = ConnectionState.DISCONNECTED

            Log.e(TAG, "Car Service connection failed", exception)
        }
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting from Car Service")

        // Invalidate callbacks from this connection.
        connectionGeneration++

        _carPropertyManager.value = null

        val currentCar = car
        car = null

        try {
            currentCar?.disconnect()
        } catch (exception: Exception) {
            Log.e(TAG, "Error while disconnecting Car Service", exception)
        }

        _connectionState.value = ConnectionState.DISCONNECTED

        Log.i(TAG, "Car Service disconnected")
    }
}
