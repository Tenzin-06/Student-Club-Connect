package com.studentclubconnect.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.Membership
import kotlinx.coroutines.tasks.await

class MembershipRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val membershipsCollection = firestore.collection("memberships")

    /**
     * Checks if a user is already a member of a club.
     */
    suspend fun checkMembership(userId: String, clubId: String): Result<Boolean> {
        return try {
            val docId = "${userId}_${clubId}"
            val document = membershipsCollection.document(docId).get().await()
            val exists = document.exists() && document.getString("status") == "active"
            Result.success(exists)
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "Error checking membership", e)
            Result.failure(e)
        }
    }

    /**
     * Adds a user to a club.
     */
    suspend fun joinClub(userId: String, clubId: String): Result<Unit> {
        return try {
            val docId = "${userId}_${clubId}"
            val membership = Membership(
                userId = userId,
                clubId = clubId,
                status = "active"
            )
            membershipsCollection.document(docId).set(membership).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "Error joining club", e)
            Result.failure(e)
        }
    }

    /**
     * Removes a user from a club by marking status as inactive or deleting the document.
     * Requirement 7: "Remove/deactivate membership". Marking inactive allows re-joining easily.
     */
    suspend fun leaveClub(userId: String, clubId: String): Result<Unit> {
        return try {
            val docId = "${userId}_${clubId}"
            // Option A: Delete the document
            membershipsCollection.document(docId).delete().await()
            
            // Option B: Mark as inactive
            // membershipsCollection.document(docId).update("status", "inactive").await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "Error leaving club", e)
            Result.failure(e)
        }
    }
}
