package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.Club
import com.studentclubconnect.data.repository.ClubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different states of the Club UI.
 */
sealed class ClubState {
    object Idle : ClubState()
    object Loading : ClubState()
    data class Success(val clubs: List<Club>) : ClubState()
    data class SingleSuccess(val club: Club?) : ClubState()
    data class ActionSuccess(val message: String) : ClubState()
    object Empty : ClubState()
    data class Error(val message: String) : ClubState()
}

/**
 * ViewModel for managing Club data and UI state.
 */
class ClubViewModel : ViewModel() {

    private val repository: ClubRepository = ClubRepository()
    
    private val _clubState = MutableStateFlow<ClubState>(ClubState.Idle)
    val clubState: StateFlow<ClubState> = _clubState.asStateFlow()

    /**
     * Fetches all clubs from the repository.
     */
    fun getClubs() {
        viewModelScope.launch {
            _clubState.value = ClubState.Loading
            val result = repository.getAllClubs()
            result.fold(
                onSuccess = { clubs ->
                    if (clubs.isEmpty()) {
                        _clubState.value = ClubState.Empty
                    } else {
                        _clubState.value = ClubState.Success(clubs)
                    }
                },
                onFailure = { error ->
                    _clubState.value = ClubState.Error("Error: ${error.message ?: "Unable to load clubs. Please try again."}") 
                }
            )
        }
    }

    /**
     * Fetches a single club by its ID.
     */
    fun getClubById(id: String) {
        viewModelScope.launch {
            _clubState.value = ClubState.Loading
            val result = repository.getClubById(id)
            result.fold(
                onSuccess = { club ->
                    if (club == null) {
                        _clubState.value = ClubState.Error("Club not found.")
                    } else {
                        _clubState.value = ClubState.SingleSuccess(club)
                    }
                },
                onFailure = { 
                    _clubState.value = ClubState.Error("Unable to load club. Please try again.") 
                }
            )
        }
    }

    /**
     * Creates a new club.
     */
    fun createClub(club: Club) {
        viewModelScope.launch {
            _clubState.value = ClubState.Loading
            val result = repository.createClub(club)
            result.fold(
                onSuccess = { _clubState.value = ClubState.ActionSuccess("Club created successfully.") },
                onFailure = { 
                    _clubState.value = ClubState.Error("Unable to create club. Please try again.") 
                }
            )
        }
    }

    /**
     * Updates an existing club.
     */
    fun updateClub(club: Club) {
        viewModelScope.launch {
            _clubState.value = ClubState.Loading
            val result = repository.updateClub(club)
            result.fold(
                onSuccess = { _clubState.value = ClubState.ActionSuccess("Club updated successfully.") },
                onFailure = { 
                    _clubState.value = ClubState.Error("Unable to update club. Please try again.") 
                }
            )
        }
    }

    /**
     * Deletes a club by its ID.
     */
    fun deleteClub(id: String) {
        viewModelScope.launch {
            _clubState.value = ClubState.Loading
            val result = repository.deleteClub(id)
            result.fold(
                onSuccess = { _clubState.value = ClubState.ActionSuccess("Club deleted successfully.") },
                onFailure = { 
                    _clubState.value = ClubState.Error("Unable to delete club. Please try again.") 
                }
            )
        }
    }

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _clubState.value = ClubState.Idle
    }
}
