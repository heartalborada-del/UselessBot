package top.griseo.bot.sdk.types.message

import top.griseo.bot.sdk.types.message.data.Message

open class MessageChain {
    private var messages: MutableList<Message>
    private constructor(messages: MutableList<Message>) {
        this.messages = messages
    }
    class Builder {
        private var messages = mutableListOf<Message>()
        fun append(message: Message): Boolean {
            return messages.add(message)
        }
        fun append(messages: Array<out Message>): Boolean {
            return this.messages.addAll(messages)
        }
        fun append(messageChain: MessageChain): Boolean {
            return messages.addAll(messageChain.messages)
        }
        fun remove(message: Message): Boolean{
            return messages.remove(message)
        }
        fun build(): MessageChain {
            return MessageChain(messages)
        }
    }
}