package io.otrade.data

import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface Message {

    fun toFrame(): Frame.Text {
        return Frame.Text(json.encodeToString(this))
    }

    val type: String

    @Serializable
    @SerialName("login")
    open class Login(
        val username: String,
        val password: String,
    ) : Message {

        @Transient
        override val type = "login"

    }

    @Serializable
    @SerialName("response")
    open class Response(
        val success: Boolean,
        val msg: String,
    ) : Message {

        @Transient
        override val type = "response"

    }

    @Serializable
    @SerialName("state")
    open class State(
        val companies: List<Company>,
        val posts: List<Post>,
    ) : Message {

        @Transient
        override val type = "state"

    }

    @Serializable
    @SerialName("decisions")
    open class Decisions(
        val decisions: List<Map<String, Decision>>
    ) : Message {

        @Transient
        override val type = "decisions"

    }

    enum class Decision {
        SELL, HOLD, BUY
    }

    companion object {
        private val json = Json
    }

}

