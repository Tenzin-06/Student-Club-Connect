package com.studentclubconnect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.studentclubconnect.data.model.Event
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Event data operations with Firebase Firestore.
 */
class EventRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val eventsCollection = firestore.collection("events")

    /**
     * Creates a new event in Firestore.
     * Generates a unique document ID and stores it in the 'id' field.
     */
    suspend fun createEvent(event: Event): Result<String> {
        return try {
            val docRef = if (event.id.isEmpty()) {
                eventsCollection.document()
            } else {
                eventsCollection.document(event.id)
            }
            
            val eventToSave = event.copy(id = docRef.id)
            docRef.set(eventToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error creating event", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all events from the 'events' collection.
     */
    suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val snapshot = eventsCollection.get().await()
            val events = snapshot.toObjects(Event::class.java)
            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting all events", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a specific event by its ID.
     */
    suspend fun getEventById(id: String): Result<Event?> {
        return try {
            val snapshot = eventsCollection.document(id).get().await()
            val event = snapshot.toObject(Event::class.java)
            Result.success(event)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting event by ID: $id", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves events belonging to a specific club.
     */
    suspend fun getEventsByClub(clubId: String): Result<List<Event>> {
        return try {
            val snapshot = eventsCollection
                .whereEqualTo("clubId", clubId)
                .get()
                .await()
            val events = snapshot.toObjects(Event::class.java)
            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting events for club: $clubId", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing event's information.
     */
    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            if (event.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("Event ID cannot be empty for update"))
            }
            eventsCollection.document(event.id).set(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error updating event: ${event.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an event by its ID.
     */
    suspend fun deleteEvent(id: String): Result<Unit> {
        return try {
            eventsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error deleting event: $id", e)
            Result.failure(e)
        }
    }
}
