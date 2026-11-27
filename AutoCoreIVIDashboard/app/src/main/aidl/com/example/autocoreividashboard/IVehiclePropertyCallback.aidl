// IVehiclePropertyCallback.aidl
package com.example.autocoreividashboard;

oneway interface IVehiclePropertyCallback {
    void onPropertyChanged(String propertyName, String value);
}
