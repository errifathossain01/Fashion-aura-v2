package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.OwnerVerificationResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for authentication screens (login, email verification, password reset).
 */
data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,

    // Email verification state
    val isEmailVerificationPending: Boolean = false,
    val unverifiedEmail: String = "",
    val resendCooldownSeconds: Int = 0,
    val isResendingVerification: Boolean = false,
    val isReloadingUser: Boolean = false,

    // Forgot password dialog state
    val isForgotPasswordDialogOpen: Boolean = false,
    val forgotPasswordEmailInput: String = "",
    val isSendingPasswordReset: Boolean = false
)

/**
 * ViewModel governing authentication interactions and state transitions.
 * Strictly decoupled from database, order management, and root navigation.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        // Observe auth state changes to detect if an already-signed-in user is unverified
        viewModelScope.launch {
            authRepository.currentAuthUserFlow.collect { user ->
                if (user != null && !user.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            isEmailVerificationPending = true,
                            unverifiedEmail = user.email ?: it.emailInput
                        )
                    }
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /**
     * Executes email & password sign-in.
     * Prevents duplicate requests while [AuthUiState.isLoading] is true.
     */
    fun signIn(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val email = currentState.emailInput.trim()
        val password = currentState.passwordInput

        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "INVALID_EMAIL") }
            return
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "EMPTY_PASSWORD") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithEmailAndPassword(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null && !user.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmailVerificationPending = true,
                            unverifiedEmail = user.email ?: email,
                            errorMessage = null
                        )
                    }
                } else if (user != null) {
                    when (val verification = authRepository.validateOwnerClaims(user)) {
                        is OwnerVerificationResult.Authorized -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                            onSuccess()
                        }
                        is OwnerVerificationResult.AccessDenied -> {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "ACCESS_DENIED_NOT_OWNER"
                                )
                            }
                        }
                        is OwnerVerificationResult.EmailNotVerified -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isEmailVerificationPending = true,
                                    unverifiedEmail = user.email ?: email,
                                    errorMessage = null
                                )
                            }
                        }
                        is OwnerVerificationResult.Unauthenticated -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "ERROR_GENERIC") }
                        }
                        is OwnerVerificationResult.Error -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = verification.message) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "ERROR_GENERIC") }
                }
            } else {
                val exception = result.exceptionOrNull()
                val mappedError = mapAuthException(exception)
                _uiState.update { it.copy(isLoading = false, errorMessage = mappedError) }
            }
        }
    }

    /**
     * Dispatches an email verification link to the current unverified user
     * with an enforced 60-second cooldown timer.
     */
    fun sendVerificationEmail() {
        val state = _uiState.value
        if (state.resendCooldownSeconds > 0 || state.isResendingVerification) return

        _uiState.update { it.copy(isResendingVerification = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isResendingVerification = false,
                        infoMessage = "VERIFICATION_EMAIL_SENT"
                    )
                }
                startCooldownTimer(60)
            } else {
                val exception = result.exceptionOrNull()
                val mappedError = mapAuthException(exception)
                _uiState.update {
                    it.copy(
                        isResendingVerification = false,
                        errorMessage = mappedError
                    )
                }
            }
        }
    }

    /**
     * Reloads the user's profile from Firebase to refresh email verification status.
     */
    fun checkEmailVerificationStatus(onVerified: () -> Unit = {}) {
        if (_uiState.value.isReloadingUser) return

        _uiState.update { it.copy(isReloadingUser = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            val result = authRepository.reloadCurrentUser()
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user?.isEmailVerified == true) {
                    when (val verification = authRepository.validateOwnerClaims(user)) {
                        is OwnerVerificationResult.Authorized -> {
                            _uiState.update {
                                it.copy(
                                    isReloadingUser = false,
                                    isEmailVerificationPending = false,
                                    infoMessage = "EMAIL_VERIFIED_SUCCESS",
                                    errorMessage = null
                                )
                            }
                            onVerified()
                        }
                        is OwnerVerificationResult.AccessDenied -> {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isReloadingUser = false,
                                    isEmailVerificationPending = false,
                                    errorMessage = "ACCESS_DENIED_NOT_OWNER"
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isReloadingUser = false,
                                    infoMessage = "EMAIL_VERIFIED_SUCCESS",
                                    errorMessage = null
                                )
                            }
                            onVerified()
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isReloadingUser = false,
                            errorMessage = "EMAIL_NOT_YET_VERIFIED"
                        )
                    }
                }
            } else {
                val exception = result.exceptionOrNull()
                val mappedError = mapAuthException(exception)
                _uiState.update {
                    it.copy(
                        isReloadingUser = false,
                        errorMessage = mappedError
                    )
                }
            }
        }
    }

    /**
     * Exits verification pending state, signs out, and returns to standard login fields.
     */
    fun cancelVerificationPending() {
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isEmailVerificationPending = false,
                unverifiedEmail = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    // --- Forgot Password Dialog ---

    fun openForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                isForgotPasswordDialogOpen = true,
                forgotPasswordEmailInput = it.emailInput.trim(),
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun closeForgotPasswordDialog() {
        _uiState.update { it.copy(isForgotPasswordDialogOpen = false) }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.update { it.copy(forgotPasswordEmailInput = email) }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (state.isSendingPasswordReset) return

        val email = state.forgotPasswordEmailInput.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "INVALID_EMAIL") }
            return
        }

        _uiState.update { it.copy(isSendingPasswordReset = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        isForgotPasswordDialogOpen = false,
                        infoMessage = "PASSWORD_RESET_SENT"
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                val mappedError = mapAuthException(exception)
                _uiState.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        errorMessage = mappedError
                    )
                }
            }
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
                delay(1000L)
                remaining--
            }
            _uiState.update { it.copy(resendCooldownSeconds = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }

    /**
     * Maps Firebase Authentication exceptions into clean symbolic codes.
     * Prevents leaking technical stack traces, passwords, or raw exceptions to the UI.
     */
    private fun mapAuthException(e: Throwable?): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "INVALID_CREDENTIALS"
            is FirebaseAuthInvalidUserException -> {
                val errorCode = e.errorCode
                if (errorCode == "ERROR_USER_DISABLED") "USER_DISABLED" else "INVALID_CREDENTIALS"
            }
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "INVALID_CREDENTIALS"
                    "ERROR_USER_DISABLED" -> "USER_DISABLED"
                    "ERROR_TOO_MANY_REQUESTS" -> "TOO_MANY_REQUESTS"
                    else -> "GENERIC_AUTH_ERROR"
                }
            }
            is FirebaseNetworkException -> "NETWORK_ERROR"
            else -> {
                if (e?.message?.contains("network", ignoreCase = true) == true) {
                    "NETWORK_ERROR"
                } else {
                    "GENERIC_AUTH_ERROR"
                }
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
