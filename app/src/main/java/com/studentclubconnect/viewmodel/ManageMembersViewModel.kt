package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentclubconnect.data.model.User
import com.studentclubconnect.data.repository.MembershipRepository
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ManageMembersState {
    object Idle : ManageMembersState()
    object Loading : ManageMembersState()
    data class Success(val members: List<User>) : ManageMembersState()
    data class ActionSuccess(val message: String) : ManageMembersState()
    object Empty : ManageMembersState()
    data class Error(val message: String) : ManageMembersState()
}

class ManageMembersViewModel : ViewModel() {

    private val membershipRepository = MembershipRepository()
    private val userRepository = UserRepository()

    private val _membersState = MutableStateFlow<ManageMembersState>(ManageMembersState.Idle)
    val membersState: StateFlow<ManageMembersState> = _membersState.asStateFlow()

    fun loadMembers(clubId: String) {
        android.util.Log.d("ManageMembers", "Loading members for club: $clubId")
        viewModelScope.launch {
            _membersState.value = ManageMembersState.Loading
            
            val membershipResult = membershipRepository.getMembershipsByClub(clubId)
            
            membershipResult.fold(
                onSuccess = { memberships ->
                    if (memberships.isEmpty()) {
                        _membersState.value = ManageMembersState.Empty
                    } else {
                        val uids = memberships.map { it.userId }
                        val userResult = userRepository.getUsersByUids(uids)
                        
                        userResult.fold(
                            onSuccess = { users ->
                                // FIX: If some profiles are missing, create "Placeholder" users 
                                // so they still show up in the list for removal.
                                val finalUserList = uids.map { uid ->
                                    users.find { it.uid == uid } ?: User(
                                        uid = uid, 
                                        name = "Unknown Member", 
                                        studentId = "ID: ${uid.take(8)}..."
                                    )
                                }
                                _membersState.value = ManageMembersState.Success(finalUserList)
                            },
                            onFailure = { error ->
                                _membersState.value = ManageMembersState.Error(error.message ?: "Failed to load member profiles")
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _membersState.value = ManageMembersState.Error(error.message ?: "Failed to load memberships")
                }
            )
        }
    }

    fun removeMember(userId: String, clubId: String) {
        viewModelScope.launch {
            _membersState.value = ManageMembersState.Loading
            
            val result = membershipRepository.leaveClub(userId, clubId)
            
            result.fold(
                onSuccess = {
                    _membersState.value = ManageMembersState.ActionSuccess("Member removed successfully")
                    // Reload the list
                    loadMembers(clubId)
                },
                onFailure = { error ->
                    _membersState.value = ManageMembersState.Error(error.message ?: "Failed to remove member")
                }
            )
        }
    }
}
