package com.studentclubconnect.utils

import android.text.format.DateUtils
import java.util.Date

/**
 * Utility for time-related operations.
 */
object TimeUtils {

    /**
     * Returns a human-readable relative time string (e.g., "Posted 2 hours ago").
     */
    fun getRelativeTime(date: Date?): String {
        if (date == null) return "Date unavailable"
        
        val now = System.currentTimeMillis()
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            date.time,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
        
        return "Posted $relativeTime"
    }
}
