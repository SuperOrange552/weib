package com.weib.app.data.realtime

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*

class RealtimeClient(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val onSecurity: (String) -> Unit,
    private val onNotification: (Long, String, String) -> Unit
) : WebSocketListener() {
    private val gson = Gson()
    private var socket: WebSocket? = null

    fun connect() {
        if (socket != null) return
        val sessionId = java.util.UUID.randomUUID().toString().replace("-", "").take(8)
        val url = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "ws/000/$sessionId/websocket"
        socket = client.newWebSocket(Request.Builder().url(url).build(), this)
    }

    fun disconnect() { socket?.close(1000, "logout"); socket = null }

    override fun onMessage(webSocket: WebSocket, text: String) {
        when {
            text == "o" -> sendFrame("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\u0000")
            text.startsWith("a[") -> decodeSockJs(text).forEach(::handleFrame)
        }
    }

    private fun handleFrame(frame: String) {
        if (frame.startsWith("CONNECTED")) {
            sendFrame("SUBSCRIBE\nid:security\ndestination:/user/queue/security\nack:auto\n\n\u0000")
            sendFrame("SUBSCRIBE\nid:notifications\ndestination:/user/queue/notifications\nack:auto\n\n\u0000")
            return
        }
        if (!frame.startsWith("MESSAGE")) return
        val body = frame.substringAfter("\n\n").trimEnd('\u0000')
        runCatching { JsonParser.parseString(body).asJsonObject }.onSuccess { event ->
            when {
                event["type"]?.asString == "FORCE_LOGOUT" -> onSecurity(event["reason"]?.asString ?: "KICKED")
                event.has("eventId") -> onNotification(event["id"].asLong, event["eventType"].asString, event["title"].asString)
            }
        }
    }

    private fun decodeSockJs(text: String): List<String> = runCatching {
        JsonParser.parseString(text.substring(1)).asJsonArray.map { it.asString }
    }.getOrDefault(emptyList())

    private fun sendFrame(frame: String) { socket?.send(gson.toJson(listOf(frame))) }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { socket = null }
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { socket = null }
}
