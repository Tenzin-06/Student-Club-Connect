package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.User
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ManageUsersState {
    object Idle : ManageUsersState()
    object Loading : ManageUsersState()
    data class Success(val users: List<User>) : ManageUsersState()
    object Empty : ManageUsersState()
    data class Error(val message: String) : ManageUsersState()
}

class ManageUsersViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _usersState = MutableStateFlow<ManageUsersState>(ManageUsersState.Idle)
    val usersState: StateFlow<ManageUsersState> = _usersState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _usersState.value = ManageUsersState.Loading
            val result = repository.getAllUsers()
            result.fold(
                onSuccess = { users ->
                    if (users.isEmpty()) {
                        _usersState.value = ManageUsersState.Empty
                    } else {
                        _usersState.value = ManageUsersState.Success(users)
                    }
                },
                onFailure = { error ->
                    _usersState.value = ManageUsersState.Error(error.message ?: "Failed to load users")
                }
            )
        }
    }
}
