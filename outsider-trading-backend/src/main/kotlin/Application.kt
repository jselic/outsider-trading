package io.otrade

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.otrade.data.Company
import io.otrade.data.Message
import io.otrade.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

val users = ConcurrentHashMap<String, WebsocketHandler>()
val agents = ConcurrentHashMap<String, WebsocketHandler>()
val companies = ConcurrentHashMap<String, Company>()
val posts = ConcurrentLinkedQueue<Pair<Post, Instant>>()

val logger: Logger = LoggerFactory.getLogger("App")

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8219) {

        DB.setup()
        companies.putAll(DB.loadCompanies().associateBy { it.id })
        launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    val sc = Scanner(System.`in`)
                    val hasNextLine = sc.hasNextLine()
                    logger.info(hasNextLine.toString())
                    if (hasNextLine) {
                        val nextLine = sc.nextLine()
                        println(nextLine)
                        if (nextLine.trim() == "stop") {
                            logger.info("Stopping server!")
                            with(this@embeddedServer) {
                                engine.stop()
                            }
                            break
                        }
                    }
                }

            }
        }
        launch {
            logger.info("Starting ticking...")
            var tick = 0
            while (true) {
                delay(2.seconds)
                tick(tick)
                tick++
            }
        }
        install(WebSockets) {
            pingPeriod = 2.seconds
            timeout = 5.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            webSocket("/agent") {
                handleAgentWebsocket()
            }
            webSocket("/user") {
                handleUserWebsocket()
            }
            install(ContentNegotiation) {
                json(Json { prettyPrint = true; isLenient = true })
            }
            install(CORS) {
                anyHost() // ⚠️ Allows all origins — use with caution in production

                anyMethod()

                allowOrigins { true }

                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowHeader("*") // Optional: allows all headers
            }
            options("{...}") {
                call.respond(HttpStatusCode.OK)
            }
            // curl -X POST http://localhost:8219/post -H "Content-Type: application/json" -d '{"poster":"test","message":"Hei testing"}'
            post("/post") {
                val post = call.receive<Post>()
                posts.add(post to Instant.now())
                logger.info("Adding post: $post")
                call.respondText("Added!", status = HttpStatusCode.Created)
            }
        }
    }.start(wait = true)
}

suspend fun tick(tick: Int) {
    logger.info("Tick #$tick!")
    posts.removeIf { Duration.between(it.second, Instant.now()) > Duration.ofSeconds(30) }
    val p = posts.map { it.first }.reversed()
    val state = Message.State(
        companies.values.map { it.copy(performance = Company.StockPerformance(it.performance.currentValue, listOf())) },
        p
    )
    logger.info("Asking ${agents.size} agents...")
    for (agent in agents.values) {
        agent.sendSafe(state)
    }
    logger.info("Waiting for agent replies...")
    val decisions = agents.values.mapNotNull { it.receiveSafe() }.flatten()
    logger.info("Processing...")
    val decisionMap = mutableMapOf<String, MutableMap<Message.Decision, Int>>()
    for (decision in decisions) {
        for ((id, d) in decision) {
            decisionMap.compute(id) { _, m -> m?.also { it.compute(d) { _, cur -> (cur ?: 0) + 1 } } ?: mutableMapOf(d to 1) }
        }
    }
    logger.info("Decisions:")
    for ((id, m) in decisionMap.entries.sortedBy { it.key }) {
        logger.info("$id -> " + m.entries.sortedBy { it.key }.joinToString(", ") { (d, c) -> "$d: $c" })
    }
    val new = companies.mapValues { (id, company) ->
        val (curr, last) = company.performance
        val d = decisionMap[id] ?: mutableMapOf<Message.Decision, Int>().also { logger.warn("No decisions received for $id?") }
        val new = adjustStockPrice(curr, d, last)
        val newLast = last.toMutableList()
        newLast.removeFirst()
        newLast.add(curr)
        logger.debug("$id - Adjusting from $curr to $new")
        logger.debug(last.joinToString(", "))
        logger.debug(newLast.joinToString(", "))
        company.copy(performance = Company.StockPerformance(new, newLast))
    }
    companies.putAll(new)
    logger.info("Results:")
    for ((id, comp) in companies.entries.sortedBy { it.key }) {
        val (curr, last) = comp.performance
        val prev = last.lastOrNull() ?: curr
        val change = curr - prev
        logger.info("$id -> $curr ($change)")
        //logger.info("$id -> $curr ($change) [${last.joinToString(", ")}]")
    }
    val newState = Message.State(
        companies.values.toList(),
        p
    )
    logger.info("Sending new state to ${users.size} users...")
    for (user in users.values) {
        user.sendSafe(newState)
    }
    logger.info("Saving state to DB...")
    DB.saveCompanies(newState.companies)
    logger.info("Tick done!")
}

suspend fun DefaultWebSocketServerSession.handleUserWebsocket() {
    val user = WebsocketHandler("User", this)
    users[user.id] = user
    val receiveJob = launch {
        try {
            incoming.receiveAsFlow().cancellable().collect {
                user.logger.warn("Got user frame?? ${it.text()}")
            }
        } catch (e: Exception) {
            user.close(e)
        }
        user.close(Throwable("Receive job ended?"))
    }
    val sendJob = launch {
        try {
            user.outgoingChannel.receiveAsFlow().cancellable().collect { msg -> send(msg) }
        } catch (e: Exception) {
            user.close(e)
        }
        user.close(Throwable("Send job ended?"))
    }

    joinAll(sendJob, receiveJob)
    user.logger.info("Stopped! Error: ${user.errorMessage}")
}


suspend fun DefaultWebSocketServerSession.handleAgentWebsocket() {
    val agent = WebsocketHandler("Agent", this)
    agents[agent.id] = agent
    val receiveJob = launch {
        try {
            incoming.receiveAsFlow().cancellable().mapNotNull {
                agent.logger.debug("Got frame: ${it.text()}")
                try {
                    return@mapNotNull Json.decodeFromString<Message.Decisions>(it.text() ?: return@mapNotNull null)
                } catch (e: Exception) {
                    return@mapNotNull null
                }
            }.collect { agent.incomingChannel.send(it) }
        } catch (e: Exception) {
            agent.close(e)
        }
        agent.close(Throwable("Receive job ended?"))
    }
    val sendJob = launch {
        try {
            agent.outgoingChannel.receiveAsFlow().cancellable().collect { msg -> send(msg) }
        } catch (e: Exception) {
            agent.close(e)
        }
        agent.close(Throwable("Send job ended?"))
    }

    joinAll(sendJob, receiveJob)
    agent.logger.info("Agent stopped! Error: ${agent.errorMessage}")
}

fun adjustStockPrice(
    currentPrice: Float,
    decisions: Map<Message.Decision, Int>,
    momentumHistory: List<Float>, // from oldest to newest
    sensitivity: Float = 3f,
    noiseLevel: Float = 0.2f // max random noise added/subtracted
): Float {
    val buy = decisions.getOrDefault(Message.Decision.BUY, 0)
    val hold = decisions.getOrDefault(Message.Decision.HOLD, 0)
    val sell = decisions.getOrDefault(Message.Decision.SELL, 0)
    val total = buy + hold + sell
    if (total == 0) return currentPrice

    // Sentiment: +1 for fully bullish, -1 for fully bearish
    val sentiment = (buy - sell).toDouble() / total

    // Momentum: average of price differences
    val momentum = if (momentumHistory.size >= 2) {
        momentumHistory.zipWithNext { a, b -> b - a }.average()
    } else 0.0

    // Influence of momentum on sentiment adjustment
    val momentumFactor = 1 + (momentum / (currentPrice + 1e-6)) // normalize
    val adjustedSentiment = sentiment * momentumFactor

    // Random noise in the range [-noiseLevel, +noiseLevel]
    val noise = Random.nextFloat() * noiseLevel * 2 - noiseLevel

    // Final price change
    val change = (adjustedSentiment * sensitivity + noise).toFloat()
    val newPrice = currentPrice + change

    return max(newPrice, 0.1f)
}

