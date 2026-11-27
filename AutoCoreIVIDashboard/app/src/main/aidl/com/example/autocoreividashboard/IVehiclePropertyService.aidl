// IVehiclePropertyService.aidl
package com.example.autocoreividashboard;

import com.example.autocoreividashboard.IVehiclePropertyCallback;

interface IVehiclePropertyService {
    String getProperty(String propertyName);
    void setProperty(String propertyName, String value);

    void registerCallback(IVehiclePropertyCallback callback);
    void unregisterCallback(IVehiclePropertyCallback callback);
}
