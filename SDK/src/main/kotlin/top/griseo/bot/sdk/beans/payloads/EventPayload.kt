package top.griseo.bot.sdk.beans.payloads

import com.google.gson.annotations.SerializedName

data class EventPayload(
    @SerializedName("op") val opcode: Long,
    @SerializedName("s") val sequence: Long? = null,
    @SerializedName("t") val type: String? = null,
    @SerializedName("d") val data: Any,
    @SerializedName("id") val id: String? = null,
)