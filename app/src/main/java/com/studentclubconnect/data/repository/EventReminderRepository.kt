package com.studentclubconnect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.EventReminder
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Event Reminder data operations with Firebase Firestore.
 */
class EventReminderRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val remindersCollection = firestore.collection("eventReminders")

    /**
     * Creates or updates a reminder for a user and event.
     * Uses userId_eventId as the document ID to prevent duplicates.
     */
    suspend fun saveReminder(reminder: EventReminder): Result<Unit> {
        return try {
            val docId = "${reminder.userId}_${reminder.eventId}"
            val reminderToSave = reminder.copy(id = docId)
            remindersCollection.document(docId).set(reminderToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventReminderRepo", "Error saving reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all reminders for a specific user.
     */
    suspend fun getRemindersByUser(userId: String): Result<List<EventReminder>> {
        return try {
            val snapshot = remindersCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val reminders = snapshot.toObjects(EventReminder::class.java)
            Result.success(reminders)
        } catch (e: Exception) {
            Log.e("EventReminderRepo", "Error getting reminders for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a user has a reminder for a specific event.
     */
    suspend fun getReminderByEvent(userId: String, eventId: String): Result<EventReminder?> {
        return try {
            val docId = "${userId}_${eventId}"
            val snapshot = remindersCollection.document(docId).get().await()
            Result.success(snapshot.toObject(EventReminder::class.java))
        } catch (e: Exception) {
            Log.e("EventReminderRepo", "Error getting reminder for event: $eventId", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a reminder using its Firestore document ID.
     */
    suspend fun deleteReminder(id: String): Result<Unit> {
        return try {
            remindersCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventReminderRepo", "Error deleting reminder: $id", e)
            Result.failure(e)
        }
    }
}
