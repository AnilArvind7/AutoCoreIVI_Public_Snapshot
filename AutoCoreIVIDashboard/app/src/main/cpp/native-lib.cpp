#include <jni.h>
#include <unordered_map>
#include <mutex>
#include <utility>
#include <cstddef>

namespace {
    constexpr jint PROPERTY_SPEED = 0x11600207;
    constexpr jint PROPERTY_BATTERY = 0x11600309;
    constexpr jint GLOBAL_AREA_ID = 0;

    struct PropertyKey {
        jint propertyId;
        jint areaId;

        bool operator==(const PropertyKey& other) const {
            return propertyId == other.propertyId &&
                   areaId == other.areaId;
        }
    };

    struct PropertyKeyHash {
        std::size_t operator()(const PropertyKey& key) const {
            const std::size_t propertyHash =
                    std::hash<jint>{}(key.propertyId);
            const std::size_t areaHash =
                    std::hash<jint>{}(key.areaId);

            return propertyHash ^ (areaHash << 1);
        }
    };

    std::unordered_map<PropertyKey, jfloat, PropertyKeyHash> propertyStore = {
            {{PROPERTY_SPEED, GLOBAL_AREA_ID}, 42.0f},
            {{PROPERTY_BATTERY, GLOBAL_AREA_ID}, 85.0f}
    };

    std::mutex propertyMutex;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_autocoreividashboard_vhal_NativeVhalBridge_nativeGetPropertyValue(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jint propertyId,
        jint areaId) {

    std::lock_guard<std::mutex> lock(propertyMutex);

    const PropertyKey key{propertyId, areaId};
    const auto it = propertyStore.find(key);

    return it != propertyStore.end() ? it->second : -1.0f;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_autocoreividashboard_vhal_NativeVhalBridge_nativeSetPropertyValue(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jint propertyId,
        jint areaId,
        jfloat value) {

    if (propertyId != PROPERTY_SPEED &&
        propertyId != PROPERTY_BATTERY) {
        return JNI_FALSE;
    }

    if (areaId != GLOBAL_AREA_ID) {
        return JNI_FALSE;
    }

    std::lock_guard<std::mutex> lock(propertyMutex);
    propertyStore[{propertyId, areaId}] = value;

    return JNI_TRUE;
}
