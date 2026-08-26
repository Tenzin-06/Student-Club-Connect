package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.User
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: User?) : ProfileState()
    data class UpdateSuccess(val message: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val repository: UserRepository = UserRepository()
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = repository.getUserProfile(uid)
            result.fold(
                onSuccess = { _profileState.value = ProfileState.Success(it) },
                onFailure = { _profileState.value = ProfileState.Error(it.message ?: "Failed to load profile") }
            )
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = repository.updateUserProfile(user)
            result.fold(
                onSuccess = { _profileState.value = ProfileState.UpdateSuccess("Profile updated successfully") },
                onFailure = { _profileState.value = ProfileState.Error(it.message ?: "Failed to update profile") }
            )
        }
    }
    
    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}