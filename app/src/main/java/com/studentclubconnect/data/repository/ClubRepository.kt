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
     * Creates a new club in Firestore.
     * If the club ID is empty, Firestore generates a unique document ID.
     */
    suspend fun createClub(club: Club): Result<String> {
        return try {
            val docRef = if (club.id.isEmpty()) {
                clubsCollection.document()
            } else {
                clubsCollection.document(club.id)
            }
            
            val clubToSave = club.copy(id = docRef.id)
            docRef.set(clubToSave).await()
            Result.success(docRef.id)
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
     * Updates an existing club's information.
     */
    suspend fun updateClub(club: Club): Result<Unit> {
        return try {
            if (club.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Club ID cannot be empty for update"))
            }
            clubsCollection.document(club.id).set(club).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error updating club: ${club.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a club by its ID.
     */
    suspend fun deleteClub(id: String): Result<Unit> {
        return try {
            clubsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ClubRepository", "Error deleting club: $id", e)
            Result.failure(e)
        }
    }
}
