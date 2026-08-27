package com.studentclubconnect.data.model

/**
 * Data model for an Event.
 * Compatible with Firestore serialization.
 */
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val clubId: String = "",
    val imageUrl: String = ""
)
