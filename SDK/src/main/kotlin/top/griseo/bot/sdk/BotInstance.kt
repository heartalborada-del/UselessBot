package top.griseo.bot.sdk

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import top.griseo.bot.sdk.exceptions.RequestException

class BotInstance(val appID: String,val secret: String,val isSandbox: Boolean = false) {
    private var CLIENT: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthorizationInterceptor(appID,secret))
        .build()
    private var BASE_URL: String = if(isSandbox) "https://sandbox.api.sgroup.qq.com" else "https://api.sgroup.qq.com/"
    private class AuthorizationInterceptor(val appID: String, val secret: String) : Interceptor {
        private var client: OkHttpClient = OkHttpClient()
        private var refreshTime = -1L
        private var expireTime = -1L
        private var accessToken: String = ""
        override fun intercept(chain: Interceptor.Chain): Response {
            if(System.currentTimeMillis() >= refreshTime + expireTime - 2000) {
                client.newCall(
                    Request.Builder()
                        .url("https://bots.qq.com/app/getAppAccessToken")
                        .post(
                            """{\"appId\":\"$appID\",\"secret\":\"$secret\"}""".toRequestBody("application/json".toMediaType())
                        )
                        .build()
                ).execute().use {
                    if(it.code != 200) throw RequestException(it.code,"Http request error")
                    else {
                        val json = JsonParser.parseString(it.body.toString()).asJsonObject
                        accessToken = json.getAsJsonPrimitive("access_token").asString
                        expireTime = json.getAsJsonPrimitive("expires_in").asLong * 1000
                        refreshTime = System.currentTimeMillis()
                    }
                }
            }
            return chain
                .proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "QQBot $accessToken")
                    .addHeader("X-Union-Appid", appID)
                    .build()
            )
        }
    }

}