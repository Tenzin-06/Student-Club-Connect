package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studentclubconnect.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MembershipState {
    object Idle : MembershipState()
    object Loading : MembershipState()
    data class Status(val isMember: Boolean) : MembershipState()
    data class Success(val message: String, val isMember: Boolean) : MembershipState()
    data class Error(val message: String) : MembershipState()
    object AuthExpired : MembershipState()
}

class MembershipViewModel : ViewModel() {

    private val repository = MembershipRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _membershipState = MutableStateFlow<MembershipState>(MembershipState.Idle)
    val membershipState: StateFlow<MembershipState> = _membershipState.asStateFlow()

    fun checkMembership(clubId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _membershipState.value = MembershipState.AuthExpired
            return
        }

        viewModelScope.launch {
            _membershipState.value = MembershipState.Loading
            repository.checkMembership(userId, clubId)
                .onSuccess { _membershipState.value = MembershipState.Status(it) }
                .onFailure { _membershipState.value = MembershipState.Error("Unable to update membership. Please try again.") }
        }
    }

    fun joinClub(clubId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _membershipState.value = MembershipState.AuthExpired
            return
        }

        viewModelScope.launch {
            _membershipState.value = MembershipState.Loading
            repository.joinClub(userId, clubId)
                .onSuccess { _membershipState.value = MembershipState.Success("Joined successfully", true) }
                .onFailure { _membershipState.value = MembershipState.Error("Unable to update membership. Please try again.") }
        }
    }

    fun leaveClub(clubId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _membershipState.value = MembershipState.AuthExpired
            return
        }

        viewModelScope.launch {
            _membershipState.value = MembershipState.Loading
            repository.leaveClub(userId, clubId)
                .onSuccess { _membershipState.value = MembershipState.Success("Left successfully", false) }
                .onFailure { _membershipState.value = MembershipState.Error("Unable to update membership. Please try again.") }
        }
    }
}
