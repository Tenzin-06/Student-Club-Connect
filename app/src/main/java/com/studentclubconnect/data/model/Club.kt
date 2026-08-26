package com.studentclubconnect.data.model

/**
 * Data model for a Student Club.
 * Compatible with Firestore serialization.
 */
data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val president: String = "",
    val imageUrl: String = ""
)
