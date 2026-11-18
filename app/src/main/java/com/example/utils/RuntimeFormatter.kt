package com.jbuilds.bingemode.utils

import java.util.Locale

/**
 * Formats a duration in minutes as a compact, human-readable runtime.
 * Examples: 121 -> "2h 1m", 90 -> "1h 30m", 60 -> "1h", 45 -> "45m".
 */
object RuntimeFormatter {
    fun format(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null

        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours == 0 -> String.format(Locale.US, "%dm", remainingMinutes)
            remainingMinutes == 0 -> String.format(Locale.US, "%dh", hours)
            else -> String.format(Locale.US, "%dh %dm", hours, remainingMinutes)
        }
    }
}
