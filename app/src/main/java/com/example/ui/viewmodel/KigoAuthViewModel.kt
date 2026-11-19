package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.KigoAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class KigoAuthViewModel(
    private val authRepository: KigoAuthRepository = KigoAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun runStartupSessionCheck(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingSession = true)
            authRepository.attemptSilentAutoSignIn(context)
            _uiState.value = _uiState.value.copy(isCheckingSession = false)
        }
    }

    fun signInWithGoogle(activity: Activity, onSuccess: (FirebaseUser) -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(
                activity = activity,
                onCancelled = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                onSuccess(user)
            }.onFailure { err ->
                val isCancel = err is androidx.credentials.exceptions.GetCredentialCancellationException
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (isCancel) null else (err.localizedMessage ?: "Sign-in failed. Please try again.")
                )
            }
        }
    }

    fun reloadUser(onComplete: (FirebaseUser?) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = authRepository.reloadCurrentUser()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                infoMessage = if (res.isSuccess) "Account verification status refreshed." else null,
                errorMessage = res.exceptionOrNull()?.localizedMessage
            )
            onComplete(res.getOrNull())
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
