package io.otrade

import io.ktor.websocket.*
import io.otrade.data.Message
import kotlinx.coroutines.channels.SendChannel

fun Frame.text(): String? {
    return (this as? Frame.Text)?.readText()?.trim()
}

suspend fun SendChannel<Frame>.send(msg: String) {
    send(Frame.Text(msg))
}

suspend fun WebSocketSession.send(msg: Message) {
    send(msg.toFrame())
}
