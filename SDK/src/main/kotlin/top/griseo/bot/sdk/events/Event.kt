package top.griseo.bot.sdk.events

open class Event

class BotOnlineEvent(val appID: String, val botName: String) : Event()
class GroupMessageEvent(val message: String) : Event()