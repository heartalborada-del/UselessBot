package top.griseo.bot.sdk.events

interface EventListener<T : Event> {
    suspend fun onEvent(event: T)
}