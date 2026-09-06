package com.studentclubconnect.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model for a Club Announcement.
 * Compatible with Firestore serialization.
 */
data class Announcement(
    val id: String = "",
    val clubId: String = "",
    val title: String = "",
    val message: String = "",
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
