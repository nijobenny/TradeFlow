package com.nijobenny.tradeflow.data

import com.nijobenny.tradeflow.core.ActivityDto
import com.nijobenny.tradeflow.core.AuthResponse
import com.nijobenny.tradeflow.core.CreateOrderRequest
import com.nijobenny.tradeflow.core.CustomerDetailDto
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.CustomerInput
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.DashboardDto
import com.nijobenny.tradeflow.core.ErrorDto
import com.nijobenny.tradeflow.core.HealthDto
import com.nijobenny.tradeflow.core.InvoiceDto
import com.nijobenny.tradeflow.core.InvoiceListDto
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.LoginRequest
import com.nijobenny.tradeflow.core.OrderDto
import com.nijobenny.tradeflow.core.OrderListDto
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.PaymentMethod
import com.nijobenny.tradeflow.core.PaymentRequest
import com.nijobenny.tradeflow.core.ProductDetailDto
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.core.ProductInput
import com.nijobenny.tradeflow.core.SearchResultDto
import com.nijobenny.tradeflow.core.StatusUpdateRequest
import com.nijobenny.tradeflow.core.StockAdjustRequest
import com.nijobenny.tradeflow.core.TeamMemberDto
import com.nijobenny.tradeflow.core.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

/** An error the user can read: either the server's message or a connection problem. */
class ApiException(val status: Int, override val message: String) : Exception(message)

/**
 * Typed client for the TradeFlow Ktor API. The engine is picked per platform
 * (OkHttp on Android, Darwin on iOS) and the request/response types come from the shared `core` module.
 */
class TradeFlowApi {
    var baseUrl: String = ""
    var token: String? = null

    /** Called when the server says the session is no longer valid. */
    var onUnauthorized: (() -> Unit)? = null

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 7_000
        }
    }

    private fun url(path: String, server: String = baseUrl) = server.trim().trimEnd('/') + "/api" + path

    private fun HttpRequestBuilder.authorize() {
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private suspend inline fun request(block: () -> HttpResponse): HttpResponse = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiException(0, "Can't reach the server at ${baseUrl.ifBlank { "this address" }}. Is it running?")
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        if (response.status.isSuccess()) return response.body()
        val message = runCatching { response.body<ErrorDto>().error }.getOrNull() ?: "Server error ${response.status.value}"
        if (response.status == HttpStatusCode.Unauthorized && token != null) onUnauthorized?.invoke()
        throw ApiException(response.status.value, message)
    }

    private suspend inline fun <reified T> getJson(path: String, vararg params: Pair<String, Any?>): T =
        decode(request { client.get(url(path)) { authorize(); params.forEach { (k, v) -> if (v != null) parameter(k, v) } } })

    private suspend inline fun <reified T, reified B : Any> postJson(path: String, body: B): T =
        decode(
            request {
                client.post(url(path)) {
                    authorize()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
        )

    private suspend inline fun <reified T, reified B : Any> putJson(path: String, body: B): T =
        decode(
            request {
                client.put(url(path)) {
                    authorize()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
        )

    // ---------- Session ----------

    /** Checks a server address before signing in. */
    suspend fun health(server: String): HealthDto =
        decode(request { client.get(url("/health", server)) })

    suspend fun login(email: String, password: String): AuthResponse = postJson("/auth/login", LoginRequest(email, password))

    suspend fun me(): UserDto = getJson("/me")

    // ---------- Dashboard ----------

    suspend fun dashboard(): DashboardDto = getJson("/dashboard")

    // ---------- Products ----------

    suspend fun products(query: String = "", category: String? = null, lowStock: Boolean = false): List<ProductDto> =
        getJson("/products", "q" to query.ifBlank { null }, "category" to category, "lowStock" to if (lowStock) "true" else null)

    suspend fun categories(): List<String> = getJson("/products/categories")

    suspend fun product(id: Int): ProductDetailDto = getJson("/products/$id")

    suspend fun createProduct(input: ProductInput): ProductDetailDto = postJson("/products", input)

    suspend fun updateProduct(id: Int, input: ProductInput): ProductDetailDto = putJson("/products/$id", input)

    suspend fun adjustStock(id: Int, delta: Int, reason: String): ProductDetailDto =
        postJson("/products/$id/stock", StockAdjustRequest(delta, reason))

    // ---------- Customers ----------

    suspend fun customers(query: String = "", tier: CustomerTier? = null): List<CustomerDto> =
        getJson("/customers", "q" to query.ifBlank { null }, "tier" to tier?.name)

    suspend fun customer(id: Int): CustomerDetailDto = getJson("/customers/$id")

    suspend fun createCustomer(input: CustomerInput): CustomerDto = postJson("/customers", input)

    suspend fun updateCustomer(id: Int, input: CustomerInput): CustomerDto = putJson("/customers/$id", input)

    // ---------- Orders ----------

    suspend fun orders(status: OrderStatus? = null, query: String = "", customerId: Int? = null): OrderListDto =
        getJson("/orders", "status" to status?.name, "q" to query.ifBlank { null }, "customerId" to customerId)

    suspend fun order(id: Int): OrderDto = getJson("/orders/$id")

    suspend fun createOrder(request: CreateOrderRequest): OrderDto = postJson("/orders", request)

    suspend fun updateOrderStatus(id: Int, status: OrderStatus, note: String = ""): OrderDto =
        postJson("/orders/$id/status", StatusUpdateRequest(status, note))

    // ---------- Invoices ----------

    suspend fun invoices(status: InvoiceStatus? = null): InvoiceListDto = getJson("/invoices", "status" to status?.name)

    suspend fun invoice(id: Int): InvoiceDto = getJson("/invoices/$id")

    suspend fun recordPayment(id: Int, amountCents: Long, method: PaymentMethod): InvoiceDto =
        postJson("/invoices/$id/payments", PaymentRequest(amountCents, method))

    // ---------- Team, activity, search ----------

    suspend fun team(): List<TeamMemberDto> = getJson("/team")

    suspend fun activity(limit: Int = 80): List<ActivityDto> = getJson("/activity", "limit" to limit)

    suspend fun search(query: String): SearchResultDto = getJson("/search", "q" to query)

    // ---------- Live feed ----------

    /**
     * Business events pushed by the server over a WebSocket. Reconnects automatically every few
     * seconds if the connection drops; [onConnected] reports the connection state for the "Live" badge.
     */
    fun liveEvents(onConnected: (Boolean) -> Unit): Flow<LiveEventDto> = channelFlow {
        while (isActive) {
            val currentToken = token
            if (currentToken != null && baseUrl.isNotBlank()) {
                val wsUrl = url("/live").replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "?token=$currentToken"
                try {
                    client.webSocket(urlString = wsUrl) {
                        onConnected(true)
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val event = runCatching { json.decodeFromString(LiveEventDto.serializer(), frame.readText()) }.getOrNull()
                                if (event != null) this@channelFlow.send(event)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // Server offline or network change: try again shortly.
                }
                onConnected(false)
            }
            delay(4_000)
        }
    }
}
