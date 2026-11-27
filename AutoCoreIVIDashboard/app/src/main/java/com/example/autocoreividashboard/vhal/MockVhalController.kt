package com.example.autocoreividashboard.vhal

import android.util.Log
import com.example.autocoreividashboard.data.VehiclePropertyStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MockVhalController(
    private val mockVhal: MockVhal
) {
    companion object {
        private const val TAG = "AutoCoreMockVHAL"
    }

    // The controller scope remains reusable across start/stop cycles.
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lifecycleLock = Any()

    private var observeJob: Job? = null

    private val _propertyEvents =
        MutableSharedFlow<VhalPropertyEvent>(
            extraBufferCapacity = 64
        )

    val propertyEvents: SharedFlow<VhalPropertyEvent> =
        _propertyEvents.asSharedFlow()

    private val previousValues =
        mutableMapOf<Pair<Int, Int>, Any>()

    private val listenerJobs =
        mutableMapOf<VhalPropertyEventListener, Job>()

    fun start() {
        synchronized(lifecycleLock) {
            if (observeJob?.isActive == true) return

            observeJob = scope.launch {
                VehiclePropertyStore.manager
                    .observeProperties()
                    .collect { properties ->
                        properties.values.forEach { property ->
                            try {
                                val converted =
                                    VhalValueConverter.convert(property)

                                val propertyKey =
                                    converted.propertyId to converted.areaId

                                val previousValue =
                                    previousValues[propertyKey]

                                // Write to Mock VHAL only when this value is new or changed.
                                if (previousValue != converted.value) {
                                    mockVhal.setProperty(property)

                                    // Emit an event only when an existing property's value changes.
                                    if (
                                        previousValue != null &&
                                        previousValue != converted.value
                                    ) {
                                        val event = VhalPropertyEvent(
                                            propertyId = converted.propertyId,
                                            areaId = converted.areaId,
                                            value = converted.value,
                                            timestampMillis =
                                                converted.timestampMillis
                                        )

                                        _propertyEvents.emit(event)
                                    }

                                    previousValues[propertyKey] =
                                        converted.value
                                }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                Log.e(
                                    TAG,
                                    "Failed to process property: ${property.key}",
                                    exception
                                )
                            }
                        }
                    }
            }

            Log.d(TAG, "Mock VHAL controller started")
        }
    }

    fun stop() {
        synchronized(lifecycleLock) {
            observeJob?.cancel()
            observeJob = null

            listenerJobs.values.forEach { it.cancel() }
            listenerJobs.clear()

            Log.d(TAG, "Mock VHAL controller stopped")
        }
    }

    fun registerListener(listener: VhalPropertyEventListener) {
        synchronized(lifecycleLock) {
            if (listenerJobs.containsKey(listener)) {
                Log.d(TAG, "Listener already registered")
                return
            }

            val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                propertyEvents.collect { event ->
                    try {
                        listener.onPropertyChanged(event)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Log.e(TAG, "Listener notification failed", exception)
                    }
                }
            }

            listenerJobs[listener] = job
            Log.d(TAG, "Listener registered")
        }
    }

    fun unregisterListener(listener: VhalPropertyEventListener) {
        synchronized(lifecycleLock) {
            val job = listenerJobs.remove(listener)
            job?.cancel()

            Log.d(TAG, "Listener unregistered")
        }
    }
}
