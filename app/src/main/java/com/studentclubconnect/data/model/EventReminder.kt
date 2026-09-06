package com.studentclubconnect.data.model

import java.util.Date

/**
 * Data model for an Event Reminder.
 * userId_eventId is typically used as the document ID to prevent duplicates.
 */
data class EventReminder(
    val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val reminderTime: Date? = null
)
