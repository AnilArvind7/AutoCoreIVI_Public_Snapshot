
package com.example.autocoreividashboard.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.autocoreividashboard.IVehiclePropertyService
import com.example.autocoreividashboard.IVehiclePropertyCallback

class VehiclePropertyServiceClient {

    private var service: IVehiclePropertyService? = null
    private var isBound = false

    private var onConnectedCallback: (() -> Unit)? = null

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(
            name: ComponentName?,
            binder: IBinder?
        ) {
            service = IVehiclePropertyService.Stub.asInterface(binder)
            onConnectedCallback?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun bind(
        context: Context,
        onConnected: () -> Unit
    ) {
        if (isBound) return

        onConnectedCallback = onConnected

        val intent = Intent(
            context,
            VehiclePropertyService::class.java
        )

        isBound = context.applicationContext.bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun getProperty(propertyName: String): String? {
        return service?.getProperty(propertyName)
    }

    fun setProperty(
        propertyName: String,
        value: String
    ) {
        service?.setProperty(propertyName, value)
    }

    fun registerCallback(callback: IVehiclePropertyCallback) {
        service?.registerCallback(callback)
    }

    fun unregisterCallback(callback: IVehiclePropertyCallback) {
        service?.unregisterCallback(callback)
    }

    fun unbind(context: Context) {
        if (isBound) {
            context.applicationContext.unbindService(connection)
            isBound = false
        }

        service = null
        onConnectedCallback = null
    }
}
