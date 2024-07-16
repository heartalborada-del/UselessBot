package top.griseo.bot.sdk.exceptions

class RequestException(code: Int, message: String) : Exception("""Some thing went wrong. Code: ${code}, Message: ${message}.""")