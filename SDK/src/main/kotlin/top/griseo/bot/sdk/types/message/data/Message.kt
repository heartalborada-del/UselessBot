package top.griseo.bot.sdk.types.message.data

import top.griseo.bot.sdk.types.message.MessageChain

interface Message {
    override fun toString(): String
    fun contentToString(): String
}