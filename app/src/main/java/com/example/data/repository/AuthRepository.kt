package com.example.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result of Firebase ID token custom claims validation for Owner authorization.
 */
sealed interface OwnerVerificationResult {
    object Unauthenticated : OwnerVerificationResult
    data class EmailNotVerified(val user: FirebaseUser) : OwnerVerificationResult
    data class Authorized(
        val user: FirebaseUser,
        val role: String,
        val merchantId: String,
        val isOffline: Boolean
    ) : OwnerVerificationResult
    data class AccessDenied(
        val user: FirebaseUser,
        val role: String?,
        val merchantId: String?,
        val email: String
    ) : OwnerVerificationResult
    data class Error(val message: String) : OwnerVerificationResult
}

/**
 * Repository wrapping Firebase Authentication primitives for email and password authentication
 * and Owner custom claims verification.
 */
class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth
) {

    companion object {
        const val REQUIRED_OWNER_ROLE = "OWNER"
        const val REQUIRED_MERCHANT_ID = "fashion_aura_main"
    }

    /**
     * Validates Firebase ID token custom claims to ensure role == "OWNER"
     * and merchantId == "fashion_aura_main".
     *
     * - Forces token refresh online.
     * - Falls back to cached claims only during genuine network failures.
     * - Denies access for missing, mismatched, or invalid claims.
     * - Never signs out merely because the device is offline.
     */
    suspend fun validateOwnerClaims(user: FirebaseUser? = auth.currentUser): OwnerVerificationResult {
        return withContext(Dispatchers.IO) {
            if (user == null) {
                return@withContext OwnerVerificationResult.Unauthenticated
            }
            if (!user.isEmailVerified) {
                return@withContext OwnerVerificationResult.EmailNotVerified(user)
            }

            try {
                // 1. Force refresh ID token online
                val tokenResult = user.getIdToken(true).awaitTask()
                val claims = tokenResult.claims
                val role = claims["role"] as? String
                val merchantId = claims["merchantId"] as? String

                if (role == REQUIRED_OWNER_ROLE && merchantId == REQUIRED_MERCHANT_ID) {
                    OwnerVerificationResult.Authorized(
                        user = user,
                        role = role,
                        merchantId = merchantId,
                        isOffline = false
                    )
                } else {
                    OwnerVerificationResult.AccessDenied(
                        user = user,
                        role = role,
                        merchantId = merchantId,
                        email = user.email ?: ""
                    )
                }
            } catch (e: Exception) {
                // 2. Strict network failure check: fall back to cached claims ONLY if genuine network failure
                if (isNetworkFailure(e)) {
                    try {
                        val cachedTokenResult = user.getIdToken(false).awaitTask()
                        val cachedClaims = cachedTokenResult.claims
                        val role = cachedClaims["role"] as? String
                        val merchantId = cachedClaims["merchantId"] as? String

                        if (role == REQUIRED_OWNER_ROLE && merchantId == REQUIRED_MERCHANT_ID) {
                            OwnerVerificationResult.Authorized(
                                user = user,
                                role = role,
                                merchantId = merchantId,
                                isOffline = true
                            )
                        } else {
                            OwnerVerificationResult.AccessDenied(
                                user = user,
                                role = role,
                                merchantId = merchantId,
                                email = user.email ?: ""
                            )
                        }
                    } catch (cachedEx: Exception) {
                        OwnerVerificationResult.Error("Offline claims verification failed: ${cachedEx.message}")
                    }
                } else {
                    // Do not treat arbitrary timeout or unknown errors as proof of offline status
                    OwnerVerificationResult.Error(e.message ?: "Claims verification error")
                }
            }
        }
    }

    private fun isNetworkFailure(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            if (current is FirebaseNetworkException ||
                current is UnknownHostException ||
                current is ConnectException ||
                current is SocketTimeoutException ||
                (current is IOException && current !is com.google.firebase.auth.FirebaseAuthException)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Observes the current Firebase user session.
     * Emits the authenticated [FirebaseUser] or null when unauthenticated.
     */
    val currentAuthUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Returns the currently cached Firebase user, or null if unauthenticated.
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Checks if a user is currently signed in and whether their email address is verified.
     */
    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    /**
     * Signs in an existing user using email and password credentials.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                if (email.isBlank() || password.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Email and password cannot be empty."))
                }
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
                val user = authResult.user
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(IllegalStateException("Authentication succeeded but returned no user profile."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Dispatches a password-reset email to the specified address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (email.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Email cannot be empty."))
                }
                auth.sendPasswordResetEmail(email.trim()).awaitTask()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Dispatches an email-verification email to the currently authenticated user.
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = auth.currentUser
                    ?: return@withContext Result.failure(IllegalStateException("No authenticated user found to send verification email."))
                user.sendEmailVerification().awaitTask()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Reloads the current user profile from Firebase to refresh claims and verification flags.
     */
    suspend fun reloadCurrentUser(): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val user = auth.currentUser
                    ?: return@withContext Result.failure(IllegalStateException("No authenticated user found to reload."))
                user.reload().awaitTask()
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Signs out the currently authenticated Firebase user.
     */
    fun signOut() {
        auth.signOut()
    }
}

/**
 * Awaits completion of a Google Play Services Task using standard Kotlin Coroutines primitives.
 * Does not require external play-services-coroutines dependencies.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            val exception = task.exception ?: IllegalStateException("Task completed unsuccessfully without an exception.")
            continuation.resumeWithException(exception)
        }
    }
}
