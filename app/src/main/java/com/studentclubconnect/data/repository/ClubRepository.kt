package com.studentclubconnect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.Club
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Club data operations with Firebase Firestore.
 */
class ClubRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val clubsCollection = firestore.collection("clubs")

    /**
     * Creates a new club in Firestore and assigns the president role.
     */
    suspend fun createClub(club: Club): Result<String> {
        return try {
            val batch = firestore.batch()
            
            val docRef = if (club.id.isEmpty()) {
                clubsCollection.document()
            } else {
                clubsCollection.document(club.id)
            }
            
            val clubId = docRef.id
            val clubToSave = club.copy(id = clubId)
            
            batch.set(docRef, clubToSave)
            
            // Assign President Role
            if (club.presidentId.isNotEmpty()) {
                val userRef = firestore.collection("users").document(club.presidentId)
                batch.update(userRef, mapOf(
                    "role" to "president",
                    "presidentOf" to clubId
                ))
            }
            
            batch.commit().await()
            Result.success(clubId)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error creating club", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all clubs from the 'clubs' collection.
     */
    suspend fun getAllClubs(): Result<List<Club>> {
        return try {
            val snapshot = clubsCollection.get().await()
            val clubs = snapshot.toObjects(Club::class.java)
            Result.success(clubs)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error getting all clubs", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a specific club by its ID.
     */
    suspend fun getClubById(id: String): Result<Club?> {
        return try {
            val snapshot = clubsCollection.document(id).get().await()
            val club = snapshot.toObject(Club::class.java)
            Result.success(club)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error getting club by ID: $id", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing club and handles president reassignment.
     */
    suspend fun updateClub(club: Club): Result<Unit> {
        return try {
            if (club.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Club ID cannot be empty for update"))
            }
            
            val batch = firestore.batch()
            
            // Get existing club to check if president changed
            val existingClubDoc = clubsCollection.document(club.id).get().await()
            val oldPresidentId = existingClubDoc.getString("presidentId") ?: ""
            
            // 1. Revert old president if changed
            if (oldPresidentId.isNotEmpty() && oldPresidentId != club.presidentId) {
                val oldUserRef = firestore.collection("users").document(oldPresidentId)
                batch.update(oldUserRef, mapOf(
                    "role" to "student",
                    "presidentOf" to null
                ))
            }
            
            // 2. Assign new president if changed
            if (club.presidentId.isNotEmpty() && club.presidentId != oldPresidentId) {
                val newUserRef = firestore.collection("users").document(club.presidentId)
                batch.update(newUserRef, mapOf(
                    "role" to "president",
                    "presidentOf" to club.id
                ))
            }
            
            // 3. Update club info
            batch.set(clubsCollection.document(club.id), club)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error updating club: ${club.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a club and reverts the president's role.
     */
    suspend fun deleteClub(id: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Get current president info before deleting
            val clubDoc = clubsCollection.document(id).get().await()
            val presidentId = clubDoc.getString("presidentId") ?: ""
            
            // 1. Delete the club
            batch.delete(clubsCollection.document(id))
            
            // 2. Revert president role
            if (presidentId.isNotEmpty()) {
                val userRef = firestore.collection("users").document(presidentId)
                batch.update(userRef, mapOf(
                    "role" to "student",
                    "presidentOf" to null
                ))
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error deleting club: $id", e)
            Result.failure(e)
        }
    }
}
