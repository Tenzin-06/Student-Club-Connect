package com.studentclubconnect.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to create user profile", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            Result.success(document.toObject(User::class.java))
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get user profile", e)
            Result.failure(e)
        }
    }
}