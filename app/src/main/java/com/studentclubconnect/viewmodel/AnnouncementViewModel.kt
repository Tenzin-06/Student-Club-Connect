package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Announcement
import com.studentclubconnect.data.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different states of the Announcement UI.
 */
sealed class AnnouncementState {
    object Idle : AnnouncementState()
    object Loading : AnnouncementState()
    data class Success(val announcements: List<Announcement>) : AnnouncementState()
    data class ActionSuccess(val message: String) : AnnouncementState()
    object Empty : AnnouncementState()
    data class Error(val message: String) : AnnouncementState()
}

/**
 * ViewModel for managing Announcement data and UI state.
 */
class AnnouncementViewModel : ViewModel() {

    private val repository: AnnouncementRepository = AnnouncementRepository()

    private val _announcementState = MutableStateFlow<AnnouncementState>(AnnouncementState.Idle)
    val announcementState: StateFlow<AnnouncementState> = _announcementState.asStateFlow()

    /**
     * Fetches all announcements.
     */
    fun getAnnouncements() {
        viewModelScope.launch {
            _announcementState.value = AnnouncementState.Loading
            val result = repository.getAllAnnouncements()
            result.fold(
                onSuccess = { announcements ->
                    if (announcements.isEmpty()) {
                        _announcementState.value = AnnouncementState.Empty
                    } else {
                        _announcementState.value = AnnouncementState.Success(announcements)
                    }
                },
                onFailure = { error ->
                    _announcementState.value = AnnouncementState.Error("Error: ${error.message ?: "Unable to load announcements."}")
                }
            )
        }
    }

    /**
     * Fetches announcements for a specific club.
     */
    fun getAnnouncementsByClub(clubId: String) {
        viewModelScope.launch {
            _announcementState.value = AnnouncementState.Loading
            val result = repository.getAnnouncementsByClub(clubId)
            result.fold(
                onSuccess = { announcements ->
                    if (announcements.isEmpty()) {
                        _announcementState.value = AnnouncementState.Empty
                    } else {
                        _announcementState.value = AnnouncementState.Success(announcements)
                    }
                },
                onFailure = { error ->
                    _announcementState.value = AnnouncementState.Error("Error: ${error.message ?: "Unable to load announcements for this club."}")
                }
            )
        }
    }

    /**
     * Creates a new announcement.
     */
    fun createAnnouncement(announcement: Announcement) {
        if (announcement.clubId.isEmpty() || announcement.title.isEmpty() || announcement.message.isEmpty() || announcement.createdBy.isEmpty()) {
            _announcementState.value = AnnouncementState.Error("All required fields must be filled.")
            return
        }

        viewModelScope.launch {
            _announcementState.value = AnnouncementState.Loading
            val result = repository.createAnnouncement(announcement)
            result.fold(
                onSuccess = { _announcementState.value = AnnouncementState.ActionSuccess("Announcement created successfully.") },
                onFailure = { error ->
                    _announcementState.value = AnnouncementState.Error("Error: ${error.message ?: "Unable to create announcement."}")
                }
            )
        }
    }

    /**
     * Updates an existing announcement.
     */
    fun updateAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            _announcementState.value = AnnouncementState.Loading
            val result = repository.updateAnnouncement(announcement)
            result.fold(
                onSuccess = { _announcementState.value = AnnouncementState.ActionSuccess("Announcement updated successfully.") },
                onFailure = { error ->
                    _announcementState.value = AnnouncementState.Error("Error: ${error.message ?: "Unable to update announcement."}")
                }
            )
        }
    }

    /**
     * Deletes an announcement.
     */
    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            _announcementState.value = AnnouncementState.Loading
            val result = repository.deleteAnnouncement(id)
            result.fold(
                onSuccess = { _announcementState.value = AnnouncementState.ActionSuccess("Announcement deleted successfully.") },
                onFailure = { error ->
                    _announcementState.value = AnnouncementState.Error("Error: ${error.message ?: "Unable to delete announcement."}")
                }
            )
        }
    }

    /**
     * Resets the UI state.
     */
    fun resetState() {
        _announcementState.value = AnnouncementState.Idle
    }
}
