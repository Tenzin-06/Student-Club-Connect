package com.studentclubconnect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.Announcement
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Announcement data operations with Firebase Firestore.
 */
class AnnouncementRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val announcementsCollection = firestore.collection("announcements")

    /**
     * Creates a new announcement in Firestore.
     */
    suspend fun createAnnouncement(announcement: Announcement): Result<String> {
        return try {
            val docRef = if (announcement.id.isEmpty()) {
                announcementsCollection.document()
            } else {
                announcementsCollection.document(announcement.id)
            }
            
            val announcementToSave = announcement.copy(id = docRef.id)
            docRef.set(announcementToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("AnnouncementRepository", "Error creating announcement", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all announcements from the 'announcements' collection.
     */
    suspend fun getAllAnnouncements(): Result<List<Announcement>> {
        return try {
            val snapshot = announcementsCollection.get().await()
            val announcements = snapshot.toObjects(Announcement::class.java)
            Result.success(announcements)
        } catch (e: Exception) {
            Log.e("AnnouncementRepository", "Error getting all announcements", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves announcements belonging to a specific club.
     */
    suspend fun getAnnouncementsByClub(clubId: String): Result<List<Announcement>> {
        return try {
            val snapshot = announcementsCollection
                .whereEqualTo("clubId", clubId)
                .get()
                .await()
            val announcements = snapshot.toObjects(Announcement::class.java)
            Result.success(announcements)
        } catch (e: Exception) {
            Log.e("AnnouncementRepository", "Error getting announcements for club: $clubId", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing announcement's information.
     */
    suspend fun updateAnnouncement(announcement: Announcement): Result<Unit> {
        return try {
            if (announcement.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Announcement ID cannot be empty for update"))
            }
            announcementsCollection.document(announcement.id).set(announcement).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AnnouncementRepository", "Error updating announcement: ${announcement.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an announcement by its ID.
     */
    suspend fun deleteAnnouncement(id: String): Result<Unit> {
        return try {
            announcementsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AnnouncementRepository", "Error deleting announcement: $id", e)
            Result.failure(e)
        }
    }
}
