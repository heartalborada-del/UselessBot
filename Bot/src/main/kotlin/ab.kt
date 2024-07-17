package top.griseo.bot.main

import top.griseo.bot.sdk.events.BotOnlineEvent
import top.griseo.bot.sdk.events.EventHandler
import top.griseo.bot.sdk.events.GroupMessageEvent
import top.griseo.bot.sdk.events.Listener

class ab : Listener() {
    @EventHandler
    fun onEvent(event: BotOnlineEvent) {
        println("Hello, World!")
    }

    @EventHandler
    fun onEvent(event: GroupMessageEvent) {
        println("Hello, World!")
    }
}