package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studentclubconnect.data.model.EventReminder
import com.studentclubconnect.data.repository.EventReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different states of the Event Reminder UI.
 */
sealed class EventReminderState {
    object Idle : EventReminderState()
    object Loading : EventReminderState()
    data class Success(val reminders: List<EventReminder>) : EventReminderState()
    data class SingleSuccess(val reminder: EventReminder?) : EventReminderState()
    data class ActionSuccess(val message: String) : EventReminderState()
    object Empty : EventReminderState()
    data class Error(val message: String) : EventReminderState()
}

/**
 * ViewModel for managing Event Reminders.
 */
class EventReminderViewModel : ViewModel() {

    private val repository: EventReminderRepository = EventReminderRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _reminderState = MutableStateFlow<EventReminderState>(EventReminderState.Idle)
    val reminderState: StateFlow<EventReminderState> = _reminderState.asStateFlow()

    /**
     * Fetches all reminders for the currently authenticated user.
     */
    fun getReminders() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _reminderState.value = EventReminderState.Loading
            val result = repository.getRemindersByUser(userId)
            result.fold(
                onSuccess = { reminders ->
                    if (reminders.isEmpty()) {
                        _reminderState.value = EventReminderState.Empty
                    } else {
                        _reminderState.value = EventReminderState.Success(reminders)
                    }
                },
                onFailure = { error ->
                    _reminderState.value = EventReminderState.Error(error.message ?: "Unable to load reminders. Please try again.")
                }
            )
        }
    }

    /**
     * Checks if a reminder exists for a specific event.
     */
    fun checkReminder(eventId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _reminderState.value = EventReminderState.Loading
            val result = repository.getReminderByEvent(userId, eventId)
            result.fold(
                onSuccess = { reminder ->
                    _reminderState.value = EventReminderState.SingleSuccess(reminder)
                },
                onFailure = { error ->
                    _reminderState.value = EventReminderState.Error(error.message ?: "Error checking reminder.")
                }
            )
        }
    }

    /**
     * Saves (creates or updates) a reminder.
     */
    fun saveReminder(reminder: EventReminder) {
        if (reminder.userId.isEmpty() || reminder.eventId.isEmpty()) {
            _reminderState.value = EventReminderState.Error("Invalid reminder data.")
            return
        }

        viewModelScope.launch {
            _reminderState.value = EventReminderState.Loading
            val result = repository.saveReminder(reminder)
            result.fold(
                onSuccess = {
                    _reminderState.value = EventReminderState.ActionSuccess("Reminder set successfully.")
                },
                onFailure = { error ->
                    _reminderState.value = EventReminderState.Error(error.message ?: "Unable to save reminder.")
                }
            )
        }
    }

    /**
     * Deletes a reminder.
     */
    fun deleteReminder(id: String) {
        viewModelScope.launch {
            _reminderState.value = EventReminderState.Loading
            val result = repository.deleteReminder(id)
            result.fold(
                onSuccess = {
                    _reminderState.value = EventReminderState.ActionSuccess("Reminder removed.")
                },
                onFailure = { error ->
                    _reminderState.value = EventReminderState.Error(error.message ?: "Unable to delete reminder.")
                }
            )
        }
    }

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _reminderState.value = EventReminderState.Idle
    }
}
