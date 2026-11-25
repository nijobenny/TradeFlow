package com.nijobenny.tradeflow.ui.theme

enum class ThemeMode { System, Light, Dark }

/** Accent palettes the user can pick in Settings. Colours are ARGB longs. */
enum class AccentTheme(val title: String, val primary: Long, val secondary: Long, val tertiary: Long) {
    Amber("Amber", 0xFFD97706, 0xFFF59E0B, 0xFFB45309),
    Copper("Copper", 0xFFC2410C, 0xFFF97316, 0xFF9A3412),
    Sage("Sage", 0xFF4D7C0F, 0xFF84CC16, 0xFF3F6212),
    Steel("Steel", 0xFF475569, 0xFF94A3B8, 0xFF334155),
    Crimson("Crimson", 0xFFB91C1C, 0xFFF87171, 0xFF7F1D1D),
}
