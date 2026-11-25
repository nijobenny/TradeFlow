package com.nijobenny.tradeflow.server

import com.nijobenny.tradeflow.core.ErrorDto
import com.nijobenny.tradeflow.core.HealthDto
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.server.db.DatabaseFactory
import com.nijobenny.tradeflow.server.db.Seeder
import com.nijobenny.tradeflow.server.db.dbQuery
import com.nijobenny.tradeflow.server.routes.authRoutes
import com.nijobenny.tradeflow.server.routes.customerRoutes
import com.nijobenny.tradeflow.server.routes.insightRoutes
import com.nijobenny.tradeflow.server.routes.invoiceRoutes
import com.nijobenny.tradeflow.server.routes.orderRoutes
import com.nijobenny.tradeflow.server.routes.productRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

const val VERSION = "1.0.0"

private val log = LoggerFactory.getLogger("TradeFlow")

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** How many apps are connected to the live feed right now. */
val liveConnections = AtomicInteger(0)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    DatabaseFactory.init()
    Seeder.run()
    printBanner(port)
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json(AppJson) }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
    }
    install(WebSockets)
    install(StatusPages) {
        exception<ApiError> { call, cause -> call.respond(cause.status, ErrorDto(cause.message)) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorDto("The request body isn't valid")) }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("Something went wrong on the server"))
        }
    }
    install(Authentication) {
        jwt("auth") {
            realm = Jwt.REALM
            verifier(Jwt.verifier)
            validate { credential ->
                if (credential.payload.getClaim("uid").asInt() != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorDto("Your session has expired. Please sign in again."))
            }
        }
    }

    routing {
        get("/") { call.respond(mapOf("name" to "TradeFlow API", "version" to VERSION, "docs" to "/api/health")) }

        route("/api") {
            get("/health") {
                call.respond(HealthDto("ok", VERSION, DatabaseFactory.description, now()))
            }
            authRoutes()

            authenticate("auth") {
                insightRoutes()
                productRoutes()
                customerRoutes()
                orderRoutes()
                invoiceRoutes()
            }

            // Live feed: ws://host:8080/api/live?token=JWT
            webSocket("/live") {
                val userId = Jwt.userIdOrNull(call.request.queryParameters["token"])
                if (userId == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                    return@webSocket
                }
                liveConnections.incrementAndGet()
                val forwarder = launch {
                    LiveBus.events.collect { event ->
                        outgoing.send(Frame.Text(AppJson.encodeToString(LiveEventDto.serializer(), event)))
                    }
                }
                try {
                    for (frame in incoming) {
                        // The app only listens; incoming frames are pings.
                    }
                } finally {
                    forwarder.cancel()
                    liveConnections.decrementAndGet()
                }
            }
        }
    }

    startDemoTraffic()
}

/**
 * While an app is watching the live feed, a new customer order arrives every few minutes and
 * the warehouse moves orders along, so the dashboard visibly updates in real time.
 * Turn it off with DEMO_TRAFFIC=off.
 */
private fun Application.startDemoTraffic() {
    if (System.getenv("DEMO_TRAFFIC") == "off") return
    launch {
        val random = Random(System.currentTimeMillis())
        while (true) {
            delay((90 + random.nextInt(120)) * 1_000L)
            if (liveConnections.get() == 0) continue
            val event = runCatching {
                dbQuery { if (random.nextInt(3) == 0) Seeder.simulatedProgress() else Seeder.simulatedOrder(random) }
            }.getOrNull()
            if (event != null) LiveBus.publish(event)
        }
    }
}

private fun printBanner(port: Int) {
    val lanAddresses = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
    }.getOrDefault(emptyList())
    val lines = buildList {
        add("")
        add("  ⚡ TradeFlow API $VERSION is running")
        add("")
        add("  Local:     http://localhost:$port")
        lanAddresses.forEach { add("  Network:   http://$it:$port   ← use this in the app on your phone (same Wi-Fi)") }
        add("  USB phone: run  adb reverse tcp:$port tcp:$port  then use http://127.0.0.1:$port")
        add("  Database:  ${DatabaseFactory.description}")
        add("  Sign in:   maya@tradeflow.dev / ${Seeder.DEMO_PASSWORD}  (also arjun, lars, sofie, noah)")
        add("")
    }
    println(lines.joinToString("\n"))
}
