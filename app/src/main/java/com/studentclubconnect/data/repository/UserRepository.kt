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

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to update user profile", e)
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

    /**
     * Retrieves all users who are currently students and not presidents.
     */
    suspend fun getEligibleStudents(): Result<List<User>> {
        return getUsersByRole("student")
    }

    /**
     * Retrieves all users with a specific role.
     */
    suspend fun getUsersByRole(role: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", role)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get users by role: $role", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves multiple user profiles by their UIDs.
     * Uses whereIn which supports up to 30 IDs per query.
     */
    suspend fun getUsersByUids(uids: List<String>): Result<List<User>> {
        if (uids.isEmpty()) return Result.success(emptyList())
        
        return try {
            val allUsers = mutableListOf<User>()
            
            for (uid in uids) {
                try {
                    val doc = firestore.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            allUsers.add(user)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UserRepository", "Error reading user document $uid", e)
                }
            }
            
            Result.success(allUsers)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to get users by UIDs", e)
            Result.failure(e)
        }
    }
}