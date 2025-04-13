package io.otrade

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.otrade.data.Message
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.util.*

private val NO_ERROR = Throwable("No error")

data class WebsocketHandler(
    val type: String,
    val websocket: DefaultWebSocketServerSession,
) {
    val id = UUID.randomUUID().toString().substring(0, 8)
    val logger = LoggerFactory.getLogger("$type - $id")
    val incomingChannel = Channel<Message.Decisions>(Channel.UNLIMITED)
    val outgoingChannel = Channel<Message.State>(Channel.UNLIMITED)
    var error: Throwable? = null
    val errorMessage: String
        get() = if (error != null && error != NO_ERROR) error!!.message ?: "Unknown Error" else "No Error"

    val isClosed: Boolean
        get() = error != null

    suspend fun sendSafe(state: Message.State) {
        try {
            outgoingChannel.send(state)
        } catch (e: Exception) {
            close(e)
        }
    }

    suspend fun receiveSafe(): List<Map<String, Message.Decision>>? {
        try {
            return incomingChannel.receive().decisions
        } catch (e: Exception) {
            close(e)
            return null
        }
    }

    suspend fun close(throwable: Throwable?) {
        if (error != null) return
        agents.remove(id)
        if (throwable == null) {
            error = NO_ERROR
            logger.info("Closing normally!")
            return
        }
        error = throwable
        logger.warn("Closing due to error: $errorMessage")
        incomingChannel.close()
        outgoingChannel.close()
        websocket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, errorMessage))
    }
}
