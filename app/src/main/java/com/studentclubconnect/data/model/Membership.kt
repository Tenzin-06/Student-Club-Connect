package com.studentclubconnect.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model for Club Membership.
 * userId_clubId is typically used as the document ID to prevent duplicates.
 */
data class Membership(
    val userId: String = "",
    val clubId: String = "",
    @ServerTimestamp
    val joinedAt: Date? = null,
    val status: String = "active" // e.g., active, inactive
)
