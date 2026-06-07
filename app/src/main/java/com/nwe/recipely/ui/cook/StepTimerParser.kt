package com.nwe.recipely.ui.cook

// Number followed by a time unit (German + English). Longer unit spellings must come
// before their abbreviations so alternation matches them first. \b after the unit keeps
// "minute" from matching inside an unrelated word.
private val TIMER_REGEX = Regex(
    """(\d+)\s*(stunden|stunde|std|hours|hour|minuten|minutes|minute|min|h)\b""",
    RegexOption.IGNORE_CASE,
)

/**
 * Parses the FIRST time duration found in a step's text and returns it in seconds,
 * or null when no usable duration is present. Hours → ×3600, minutes → ×60.
 */
fun parseTimerSeconds(text: String): Int? {
    val match = TIMER_REGEX.find(text) ?: return null
    val value = match.groupValues[1].toIntOrNull() ?: return null
    if (value <= 0) return null
    val unit = match.groupValues[2].lowercase()
    val isHour = unit.startsWith("h") || unit.startsWith("s") // hour(s)/h, std/stunde(n)
    return if (isHour) value * 3600 else value * 60
}
