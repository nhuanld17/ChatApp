package com.example.dacs.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class ProfileState {
    object Initial : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Initial)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _displayName = MutableStateFlow(Firebase.auth.currentUser?.displayName ?: "")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _email = MutableStateFlow(Firebase.auth.currentUser?.email ?: "")
    val email: StateFlow<String> = _email.asStateFlow()

    fun updateDisplayName(newDisplayName: String) {
        _displayName.value = newDisplayName
    }

    fun updateProfile() {
        viewModelScope.launch {
            try {
                _state.value = ProfileState.Loading
                val user = Firebase.auth.currentUser
                user?.updateProfile(userProfileChangeRequest {
                    displayName = _displayName.value
                })?.await()
                _state.value = ProfileState.Success
            } catch (e: Exception) {
                _state.value = ProfileState.Error(e.message ?: "Failed to update profile")
            }
        }
    }
} 