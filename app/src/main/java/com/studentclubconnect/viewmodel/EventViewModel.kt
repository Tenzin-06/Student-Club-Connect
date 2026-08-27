package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Event
import com.studentclubconnect.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different states of the Event UI.
 */
sealed class EventState {
    object Idle : EventState()
    object Loading : EventState()
    data class Success(val events: List<Event>) : EventState()
    data class SingleSuccess(val event: Event?) : EventState()
    data class ActionSuccess(val message: String) : EventState()
    object Empty : EventState()
    data class Error(val message: String) : EventState()
}

/**
 * ViewModel for managing Event data and UI state.
 */
class EventViewModel : ViewModel() {

    private val repository: EventRepository = EventRepository()

    private val _eventState = MutableStateFlow<EventState>(EventState.Idle)
    val eventState: StateFlow<EventState> = _eventState.asStateFlow()

    /**
     * Fetches all events from the repository.
     */
    fun getEvents() {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.getAllEvents()
            result.fold(
                onSuccess = { events ->
                    if (events.isEmpty()) {
                        _eventState.value = EventState.Empty
                    } else {
                        _eventState.value = EventState.Success(events)
                    }
                },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to load events. Please try again."}")
                }
            )
        }
    }

    /**
     * Fetches a single event by its ID.
     */
    fun getEventById(id: String) {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.getEventById(id)
            result.fold(
                onSuccess = { event ->
                    if (event == null) {
                        _eventState.value = EventState.Error("Event not found.")
                    } else {
                        _eventState.value = EventState.SingleSuccess(event)
                    }
                },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to load event. Please try again."}")
                }
            )
        }
    }

    /**
     * Fetches events belonging to a specific club.
     */
    fun getEventsByClub(clubId: String) {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.getEventsByClub(clubId)
            result.fold(
                onSuccess = { events ->
                    if (events.isEmpty()) {
                        _eventState.value = EventState.Empty
                    } else {
                        _eventState.value = EventState.Success(events)
                    }
                },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to load events for this club. Please try again."}")
                }
            )
        }
    }

    /**
     * Creates a new event.
     */
    fun createEvent(event: Event) {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.createEvent(event)
            result.fold(
                onSuccess = { _eventState.value = EventState.ActionSuccess("Event created successfully.") },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to create event. Please try again."}")
                }
            )
        }
    }

    /**
     * Updates an existing event.
     */
    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.updateEvent(event)
            result.fold(
                onSuccess = { _eventState.value = EventState.ActionSuccess("Event updated successfully.") },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to update event. Please try again."}")
                }
            )
        }
    }

    /**
     * Deletes an event by its ID.
     */
    fun deleteEvent(id: String) {
        viewModelScope.launch {
            _eventState.value = EventState.Loading
            val result = repository.deleteEvent(id)
            result.fold(
                onSuccess = { _eventState.value = EventState.ActionSuccess("Event deleted successfully.") },
                onFailure = { error ->
                    _eventState.value = EventState.Error("Error: ${error.message ?: "Unable to delete event. Please try again."}")
                }
            )
        }
    }

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _eventState.value = EventState.Idle
    }
}
