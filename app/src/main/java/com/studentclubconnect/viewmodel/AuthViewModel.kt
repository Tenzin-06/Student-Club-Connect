package com.studentclubconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.studentclubconnect.data.model.User
import com.studentclubconnect.data.repository.AuthRepository
import com.studentclubconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val authRepository: AuthRepository = AuthRepository()
    private val userRepository: UserRepository = UserRepository()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(uid).onSuccess { user ->
                _userProfile.value = user
            }.onFailure {
                android.util.Log.e("AuthViewModel", "Failed to load user profile", it)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadUserProfile(firebaseUser.uid)
                    }
                    _authState.value = AuthState.Success(firebaseUser)
                },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun signUp(email: String, name: String, studentId: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Step 1: Create Auth Account
            val authResult = authRepository.signUp(email, password)
            
            authResult.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        // Step 2: Create Firestore Profile
                        val userProfile = User(
                            uid = firebaseUser.uid,
                            name = name,
                            studentId = studentId,
                            email = email,
                            role = "student" // Default role
                        )
                        
                        val firestoreResult = userRepository.createUserProfile(userProfile)
                        
                        firestoreResult.fold(
                            onSuccess = { 
                                _userProfile.value = userProfile
                                _authState.value = AuthState.Success(firebaseUser) 
                            },
                            onFailure = { 
                                _authState.value = AuthState.Error("Account created, but we couldn't save your profile. Please try again.") 
                            }
                        )
                    } else {
                        _authState.value = AuthState.Error("Signup failed: User is null")
                    }
                },
                onFailure = { 
                    _authState.value = AuthState.Error(it.message ?: "Signup failed") 
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _userProfile.value = null
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}