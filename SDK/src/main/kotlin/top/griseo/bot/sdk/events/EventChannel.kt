package top.griseo.bot.sdk.events

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class EventManager {
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    private val listeners = mutableMapOf<Class<out Event>, MutableList<EventListener<out Event>>>()

    fun <T : Event> registerListener(eventClass: Class<T>, listener: EventListener<T>) {
        listeners.computeIfAbsent(eventClass) { mutableListOf() }.add(listener)
    }

    fun <T : Event> unregisterListener(eventClass: Class<T>, listener: EventListener<T>) {
        listeners[eventClass]?.remove(listener)
    }

    suspend fun <T : Event> fireEvent(event: T) {
        eventChannel.send(event)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startEventLoop() = GlobalScope.launch {
        for (event in eventChannel) {
            listeners[event::class.java]?.forEach { listener ->
                @Suppress("UNCHECKED_CAST")
                (listener as? EventListener<Event>)?.onEvent(event)
            }
        }
    }
}