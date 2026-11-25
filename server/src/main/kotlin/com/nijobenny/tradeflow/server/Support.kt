package com.nijobenny.tradeflow.server

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.Permissions
import com.nijobenny.tradeflow.core.Role
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.server.db.ActivityLog
import com.nijobenny.tradeflow.server.db.Users
import com.nijobenny.tradeflow.server.db.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.exposed.sql.*
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

/** Thrown from route code; StatusPages turns it into `{"error": message}` with [status]. */
class ApiError(val status: HttpStatusCode, override val message: String) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw ApiError(HttpStatusCode.BadRequest, message)
fun notFound(what: String): Nothing = throw ApiError(HttpStatusCode.NotFound, "$what not found")
fun conflict(message: String): Nothing = throw ApiError(HttpStatusCode.Conflict, message)
fun forbidden(message: String = "Your role can't do that"): Nothing = throw ApiError(HttpStatusCode.Forbidden, message)

fun now(): Long = System.currentTimeMillis()

const val DAY_MS = 24L * 60 * 60 * 1000

object Jwt {
    private const val ISSUER = "tradeflow"
    private const val VALIDITY_MS = 30 * DAY_MS
    private val algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET") ?: "tradeflow-dev-secret-change-me")

    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(ISSUER).build()

    const val REALM = "TradeFlow"

    fun issue(userId: Int, role: Role): String = JWT.create()
        .withIssuer(ISSUER)
        .withClaim("uid", userId)
        .withClaim("role", role.name)
        .withExpiresAt(Date(now() + VALIDITY_MS))
        .sign(algorithm)

    /** User id from a raw token (used by the WebSocket, which can't send headers from every client). */
    fun userIdOrNull(token: String?): Int? = token?.let {
        runCatching { verifier.verify(it).getClaim("uid").asInt() }.getOrNull()
    }
}

object Passwords {
    fun hash(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt(10))
    fun matches(plain: String, hash: String): Boolean = runCatching { BCrypt.checkpw(plain, hash) }.getOrDefault(false)
}

fun ResultRow.toUserDto() = UserDto(
    id = this[Users.id],
    name = this[Users.name],
    email = this[Users.email],
    role = this[Users.role],
    title = this[Users.title],
)

/** The signed-in user for this request, loaded fresh so role changes apply immediately. */
suspend fun ApplicationCall.currentUser(): UserDto {
    val id = principal<JWTPrincipal>()?.payload?.getClaim("uid")?.asInt()
        ?: throw ApiError(HttpStatusCode.Unauthorized, "Please sign in")
    return dbQuery { Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUserDto() }
        ?: throw ApiError(HttpStatusCode.Unauthorized, "Account no longer exists")
}

/** Loads the user and checks they're allowed to do [permission]. */
suspend fun ApplicationCall.requirePermission(permission: Permission): UserDto {
    val user = currentUser()
    if (!Permissions.allowed(user.role, permission)) forbidden("${user.role.label} users can't do that")
    return user
}

fun ApplicationCall.intParam(name: String = "id"): Int =
    parameters[name]?.toIntOrNull() ?: badRequest("Invalid $name")

/** Broadcasts business events to every connected app over the WebSocket. */
object LiveBus {
    private val flow = MutableSharedFlow<LiveEventDto>(extraBufferCapacity = 64)
    val events: SharedFlow<LiveEventDto> = flow.asSharedFlow()

    fun publish(event: LiveEventDto) {
        flow.tryEmit(event)
    }
}

/**
 * Writes an audit-log entry inside the current transaction and returns the matching live event,
 * which the caller publishes once the transaction has committed.
 */
fun recordActivity(
    type: LiveEventType,
    message: String,
    user: UserDto,
    entity: String,
    entityId: Int,
    at: Long = now(),
): LiveEventDto {
    ActivityLog.insert {
        it[ActivityLog.type] = type
        it[ActivityLog.message] = message
        it[ActivityLog.userId] = user.id
        it[ActivityLog.entity] = entity
        it[ActivityLog.entityId] = entityId
        it[ActivityLog.createdAt] = at
    }
    return LiveEventDto(type, message, entity, entityId, user.name, at)
}
