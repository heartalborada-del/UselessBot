package top.griseo.bot.sdk.beans.payloads

import com.google.gson.annotations.SerializedName

data class IdentifyPayload(
    @SerializedName("token") val token: String,
    @SerializedName("intents") val intents: Long,
    @SerializedName("shard") val shard: List<Long> = listOf(0, 1),
)
