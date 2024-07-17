package top.griseo.bot.sdk.events

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

class EventChannel {
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
    private val listeners = mutableMapOf<Class<out Event>, MutableList<EventListener<out Event>>>()
    private val kvListeners = mutableMapOf<Class<out Listener>, MutableList<EventListener<out Event>>>()
    fun <T : Event> registerListener(eventClass: Class<T>, listener: EventListener<T>) {
        listeners.computeIfAbsent(eventClass) { mutableListOf() }.add(listener)
    }

    fun <T : Event> unregisterListener(eventClass: Class<T>, listener: EventListener<T>) {
        listeners[eventClass]?.remove(listener)
    }

    suspend fun <T : Event> fireEvent(event: T) {
        eventChannel.send(event)
    }

    fun registerListeners(listener: Listener) {
        listener::class.declaredFunctions.forEach { function ->
            function.findAnnotation<EventHandler>()?.let { annotation ->
                val parameters = function.parameters
                if (parameters.size == 2 && Event::class.java.isAssignableFrom(parameters[1].type.jvmErasure.java)) {
                    @Suppress("UNCHECKED_CAST")
                    val eventClass = parameters[1].type.jvmErasure.java as Class<out Event>
                    val l = object : EventListener<Event> {
                        override suspend fun onEvent(event: Event) {
                            function.callSuspend(listener, event)
                        }
                    }
                    this.kvListeners.computeIfAbsent(listener::class.java) { mutableListOf() }.add(l)
                    this.listeners.computeIfAbsent(eventClass) { mutableListOf() }.add(l)
                }
            }

        }
    }

    fun unregisterListeners(listener: Listener) {
        listener::class.declaredFunctions.forEach { function ->
            function.findAnnotation<EventHandler>()?.let {
                val parameters = function.parameters
                if (parameters.size == 2 && Event::class.java.isAssignableFrom(parameters[1].type.jvmErasure.java)) {
                    @Suppress("UNCHECKED_CAST")
                    val eventClass = parameters[1].type.jvmErasure.java as Class<out Event>
                    kvListeners[listener::class.java]?.forEach {
                        listeners[eventClass]?.remove(it)
                    }
                }
            }
        }
        kvListeners.remove(listener::class.java)
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
