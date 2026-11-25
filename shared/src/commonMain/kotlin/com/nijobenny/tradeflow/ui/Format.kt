package com.nijobenny.tradeflow.ui

import androidx.compose.ui.graphics.Color
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.Permissions
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.platform.currentTimeMillis
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun money(cents: Long): String = Money.format(cents)
fun moneyShort(cents: Long): String = Money.format(cents, showCents = false)

private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "23 Nov 2026" */
@OptIn(ExperimentalTime::class)
fun dateLabel(epochMillis: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val iso = t.date.toString() // yyyy-MM-dd
    return "${iso.substringAfterLast('-').trimStart('0')} ${months[t.month.ordinal]} ${iso.substringBefore('-')}"
}

/** "23 Nov, 14:05" */
@OptIn(ExperimentalTime::class)
fun dateTimeLabel(epochMillis: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = t.date.toString().substringAfterLast('-').trimStart('0')
    return "$day ${months[t.month.ordinal]}, ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

/** "just now", "5m ago", "3h ago", "2d ago", then a date. */
fun timeAgo(epochMillis: Long, now: Long = currentTimeMillis()): String {
    val seconds = (now - epochMillis) / 1000
    return when {
        seconds < 45 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        seconds < 7 * 86_400 -> "${seconds / 86_400}d ago"
        else -> dateLabel(epochMillis)
    }
}

/** "in 5 days", "due today", "12 days overdue". */
fun dueLabel(dueAt: Long, now: Long = currentTimeMillis()): String {
    val days = ((dueAt - now) / 86_400_000L).toInt()
    return when {
        days > 1 -> "Due in $days days"
        days == 1 -> "Due tomorrow"
        days == 0 -> "Due today"
        else -> "${-days} days overdue"
    }
}

fun percentLabel(bp: Int): String = "${bp / 100}.${(bp % 100 / 10)}%"

fun UserDto?.can(permission: Permission): Boolean = this != null && Permissions.allowed(role, permission)

fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

fun OrderStatus.color(): Color = when (this) {
    OrderStatus.PENDING -> Color(0xFFF59E0B)
    OrderStatus.CONFIRMED -> Color(0xFF3B82F6)
    OrderStatus.PACKED -> Color(0xFFA855F7)
    OrderStatus.SHIPPED -> Color(0xFF06B6D4)
    OrderStatus.DELIVERED -> Color(0xFF22C55E)
    OrderStatus.CANCELLED -> Color(0xFFEF4444)
}

fun OrderStatus.emoji(): String = when (this) {
    OrderStatus.PENDING -> "🕒"
    OrderStatus.CONFIRMED -> "✅"
    OrderStatus.PACKED -> "📦"
    OrderStatus.SHIPPED -> "🚚"
    OrderStatus.DELIVERED -> "🏁"
    OrderStatus.CANCELLED -> "✖️"
}

/** The button label for moving an order to this status. */
fun OrderStatus.actionLabel(): String = when (this) {
    OrderStatus.CONFIRMED -> "Confirm order"
    OrderStatus.PACKED -> "Mark as packed"
    OrderStatus.SHIPPED -> "Mark as shipped"
    OrderStatus.DELIVERED -> "Mark as delivered"
    OrderStatus.CANCELLED -> "Cancel order"
    OrderStatus.PENDING -> "Reopen"
}

fun InvoiceStatus.color(): Color = when (this) {
    InvoiceStatus.UNPAID -> Color(0xFF3B82F6)
    InvoiceStatus.PARTIAL -> Color(0xFFF59E0B)
    InvoiceStatus.PAID -> Color(0xFF22C55E)
    InvoiceStatus.OVERDUE -> Color(0xFFEF4444)
    InvoiceStatus.VOID -> Color(0xFF94A3B8)
}

fun CustomerTier.colors(): List<Color> = when (this) {
    CustomerTier.BRONZE -> listOf(Color(0xFFB45309), Color(0xFFF59E0B))
    CustomerTier.SILVER -> listOf(Color(0xFF64748B), Color(0xFFCBD5E1))
    CustomerTier.GOLD -> listOf(Color(0xFFD97706), Color(0xFFFACC15))
    CustomerTier.PLATINUM -> listOf(Color(0xFF7C3AED), Color(0xFF22D3EE))
}

fun CustomerTier.emoji(): String = when (this) {
    CustomerTier.BRONZE -> "🥉"
    CustomerTier.SILVER -> "🥈"
    CustomerTier.GOLD -> "🥇"
    CustomerTier.PLATINUM -> "💎"
}

fun LiveEventType.emoji(): String = when (this) {
    LiveEventType.ORDER_CREATED -> "🛒"
    LiveEventType.ORDER_STATUS -> "🚚"
    LiveEventType.PAYMENT -> "💶"
    LiveEventType.STOCK -> "📦"
    LiveEventType.PRODUCT -> "🏷️"
    LiveEventType.CUSTOMER -> "🤝"
}

/** A stable colour per person for avatars. */
fun avatarColors(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF6366F1), Color(0xFF22D3EE)),
        listOf(Color(0xFFF43F5E), Color(0xFFFB923C)),
        listOf(Color(0xFF10B981), Color(0xFF84CC16)),
        listOf(Color(0xFFA855F7), Color(0xFFEC4899)),
        listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    )
    return palettes[(name.hashCode() and 0x7fffffff) % palettes.size]
}
