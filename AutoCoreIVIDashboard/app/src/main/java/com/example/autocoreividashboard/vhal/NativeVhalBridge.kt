package com.example.autocoreividashboard.vhal

object NativeVhalBridge {

    init {
        System.loadLibrary("autocore_vhal")
    }

    external fun nativeGetPropertyValue(
        propertyId: Int,
        areaId: Int
    ): Float

    external fun nativeSetPropertyValue(
        propertyId: Int,
        areaId: Int,
        value: Float
    ): Boolean
}
