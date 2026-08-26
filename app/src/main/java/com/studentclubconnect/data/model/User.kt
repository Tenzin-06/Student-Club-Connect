package com.studentclubconnect.data.model

import com.google.firebase.firestore.ServerTimestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val studentId: String = "",
    val email: String = "",
    val programme: String = "",
    val department: String = "",
    val semester: Int = 0,
    val role: String = "student",
    @ServerTimestamp
    val createdAt: java.util.Date? = null
)