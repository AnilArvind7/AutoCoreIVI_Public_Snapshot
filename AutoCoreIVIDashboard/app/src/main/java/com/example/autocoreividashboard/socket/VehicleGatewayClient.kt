package com.example.autocoreividashboard.socket

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VehicleState(
    val speed: Int,
    val battery: Int,
    val ignitionOn: Boolean,
    val headlightsOn: Boolean,
    val headlightBeam: String = "LOW",
    val hvacOn: Boolean,
    val temperature: Int,
    val gear: String = "P",
    val frontLeftDoorOpen: Boolean = false,
    val frontRightDoorOpen: Boolean = false,
    val rearLeftDoorOpen: Boolean = false,
    val rearRightDoorOpen: Boolean = false
)

class VehicleGatewayClient(
    private val onConnected: () -> Unit,
    private val onStateReceived: (VehicleState) -> Unit,
    private val onError: (String) -> Unit,
    private val onDisconnected: () -> Unit
) {
    companion object {
        private const val TAG = "AutoCoreIVI"
        private const val GATEWAY_URL = "ws://10.0.2.2:8765"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(GATEWAY_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Connected to vehicle gateway")
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)

                    if (json.optString("type") == "error") {
                        onError(json.optString("message", "Gateway error"))
                        return
                    }

                    if (json.optString("type") != "vehicle_state") return

                    val state = VehicleState(
                        speed = json.getInt("speed"),
                        battery = json.getInt("battery"),
                        ignitionOn = json.getBoolean("ignitionOn"),
                        headlightsOn = json.getBoolean("headlightsOn"),
                        headlightBeam = json.optString("headlightBeam", "LOW"),
                        hvacOn = json.getBoolean("hvacOn"),
                        temperature = json.getInt("temperature"),
                        gear = json.optString("gear", "P"),
                        frontLeftDoorOpen = json.optBoolean("frontLeftDoorOpen", false),
                        frontRightDoorOpen = json.optBoolean("frontRightDoorOpen", false),
                        rearLeftDoorOpen = json.optBoolean("rearLeftDoorOpen", false),
                        rearRightDoorOpen = json.optBoolean("rearRightDoorOpen", false)
                    )

                    onStateReceived(state)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse vehicle state", e)
                    onError("Invalid vehicle-state message")
                }
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                this@VehicleGatewayClient.webSocket = null

                Log.e(TAG, "Gateway connection failed", t)

                onError(t.message ?: "Gateway connection failed")
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                this@VehicleGatewayClient.webSocket = null
                Log.i(TAG, "Gateway connection closed: $reason")
                onDisconnected()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Dashboard closed")
        webSocket = null
        client.dispatcher.cancelAll()
    }
}
