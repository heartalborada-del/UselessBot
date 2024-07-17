package top.griseo.bot.sdk

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import top.griseo.bot.sdk.beans.payloads.EventPayload
import top.griseo.bot.sdk.beans.payloads.IdentifyPayload
import top.griseo.bot.sdk.events.BotOnlineEvent
import top.griseo.bot.sdk.events.EventChannel
import top.griseo.bot.sdk.events.GroupMessageEvent
import top.griseo.bot.sdk.exceptions.RequestException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicLong


class BotInstance(private val appID: String, secret: String, isSandbox: Boolean = false) {
    private var interceptor = AuthorizationInterceptor(appID, secret)
    private var client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
    private var baseUrl: String = if (isSandbox) "https://sandbox.api.sgroup.qq.com" else "https://api.sgroup.qq.com"
    var eventChannel = EventChannel()

    private class AuthorizationInterceptor(val appID: String, val secret: String) : Interceptor {
        private var internalClient: OkHttpClient = OkHttpClient()
        private var refreshTime = -1L
        private var expireTime = -1L
        private var accessToken: String = ""
        fun getAccessToken(): String {
            if (System.currentTimeMillis() >= refreshTime + expireTime - 2000) {
                internalClient.newCall(
                    Request.Builder()
                        .url("https://bots.qq.com/app/getAppAccessToken")
                        .post(
                            """{"appId":"$appID","clientSecret":"$secret"}""".toRequestBody("application/json".toMediaType())
                        )
                        .build()
                ).execute().use {
                    if (it.code != 200 || it.body == null) throw RequestException(it.code, "Http request error")
                    else {
                        val json = JsonParser.parseString(it.body!!.charStream().readText()).asJsonObject
                        if (json.has("code") && json.getAsJsonPrimitive("code").asInt != 0)
                            throw RequestException(
                                json.getAsJsonPrimitive("code").asInt,
                                json.getAsJsonPrimitive("message").asString
                            )
                        accessToken = json.getAsJsonPrimitive("access_token").asString
                        expireTime = json.getAsJsonPrimitive("expires_in").asLong * 1000
                        refreshTime = System.currentTimeMillis()
                    }
                }
            }
            return accessToken
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val resp = chain
                .proceed(
                    chain.request().newBuilder()
                        .addHeader("Authorization", "QQBot ${getAccessToken()}")
                        .addHeader("X-Union-Appid", appID)
                        .build()
                )
            if (resp.body == null) {
                throw RequestException(resp.code, "Http request error")
            } else if (resp.code != 101) {
                val source: BufferedSource = resp.body!!.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                val result = buffer.clone().readString(Charset.defaultCharset())
                val json = JsonParser.parseString(result).asJsonObject
                if (json.has("code") && json.getAsJsonPrimitive("code").asInt != 0)
                    throw RequestException(
                        json.getAsJsonPrimitive("code").asInt,
                        json.getAsJsonPrimitive("message").asString
                    )
            }
            return resp
        }
    }

    private class PayloadProcessor(
        val interceptor: AuthorizationInterceptor,
        val eventChannel: EventChannel,
        val appID: String
    ) : WebSocketListener() {
        private var latestSerial = AtomicLong(-1)
        private var heartbeatTimer: Timer = Timer()
        private var interval: Long = -1L

        @OptIn(DelicateCoroutinesApi::class)
        override fun onMessage(webSocket: WebSocket, text: String) {
            GlobalScope.launch {
                eventChannel.fireEvent(GroupMessageEvent("awa"))
            }
            println(text)
            val payload = Gson().fromJson(text, EventPayload::class.java)
            if (payload.sequence != null && payload.sequence > latestSerial.get() && payload.sequence != 0L)
                latestSerial.set(payload.sequence)
            when (payload.opcode) {
                10L -> {
                    // Connected
                    interval =
                        JsonParser.parseString(Gson().toJson(payload.data)).asJsonObject.getAsJsonPrimitive("heartbeat_interval").asLong
                    val data = EventPayload(
                        opcode = 2,
                        data = IdentifyPayload(
                            "QQBot ${interceptor.getAccessToken()}",
                            0 or (1 shl 30) or (1 shl 1) or (1 shl 25)
                        )
                    )
                    webSocket.send(Gson().toJson(data))
                }

                0L -> {
                    when (payload.type) {
                        "READY" -> {
                            GlobalScope.launch {
                                val json = JsonParser.parseString(Gson().toJson(payload.data)).asJsonObject
                                eventChannel.fireEvent(
                                    BotOnlineEvent(
                                        appID,
                                        json.getAsJsonObject("user").getAsJsonPrimitive("username").asString
                                    )
                                )
                            }
                            // Heartbeat Schedule
                            heartbeatTimer.schedule(object : TimerTask() {
                                override fun run() {
                                    val serial: String =
                                        if (latestSerial.get() == -1L) "null" else latestSerial.get().toString()
                                    webSocket.send("""{"op":1,"d":${serial}}""")
                                }
                            }, 0, interval)
                        }
                    }
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            heartbeatTimer.cancel()
            super.onClosing(webSocket, code, reason)
        }
    }

    private fun listenWebsocket() {
        client.newCall(Request.Builder().url("""${baseUrl}/gateway""").get().build()).execute().use {
            val json = JsonParser.parseString(it.body!!.charStream().readText()).asJsonObject
            val url = json.getAsJsonPrimitive("url").asString
            client.newWebSocket(Request.Builder().url(url).build(), PayloadProcessor(interceptor, eventChannel, appID))
        }
    }

    init {
        eventChannel.startEventLoop()
        listenWebsocket()
    }
}