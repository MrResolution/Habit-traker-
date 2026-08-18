package com.example.ui

import android.app.Application
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object NeedsRegistration : AuthState()
    object NeedsLogin : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
        // Web client ID from google-services.json (client_type 3)
        const val WEB_CLIENT_ID = "488253985748-3bb2kt7qf7kcsgg3qhq8bjgj92tdej41.apps.googleusercontent.com"
    }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        } else {
            _authState.value = AuthState.NeedsLogin
        }
    }

    fun register(displayName: String, email: String, password: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _loginError.value = "All fields are required"
            return
        }
        if (password.length < 6) {
            _loginError.value = "Password must be at least 6 characters"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    // Set display name
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                    }
                    user.updateProfile(profileUpdates).await()

                    // Create user document in Firestore
                    createUserDocument(user, displayName)

                    _authState.value = AuthState.Authenticated(user)
                    _loginError.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed", e)
                _loginError.value = e.localizedMessage ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Email and password cannot be empty"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                    _loginError.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                _loginError.value = e.localizedMessage ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(activityContext: android.app.Activity) {
        _isLoading.value = true
        _loginError.value = null
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activityContext)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )

                handleGoogleSignInResult(result)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.d(TAG, "Google Sign-In cancelled by user")
                _loginError.value = null
                _isLoading.value = false
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                Log.e(TAG, "No Google accounts available", e)
                _loginError.value = "No Google account found on this device"
                _isLoading.value = false
            } catch (e: androidx.credentials.exceptions.GetCredentialProviderConfigurationException) {
                Log.e(TAG, "Google Sign-In provider configuration exception", e)
                _loginError.value = "Google Sign-In is not configured properly"
                _isLoading.value = false
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed", e)
                _loginError.value = "Google Sign-In failed: ${e.localizedMessage}"
                _isLoading.value = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed unexpectedly", e)
                _loginError.value = e.localizedMessage ?: "Google Sign-In failed"
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        val idToken = try {
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Google ID token", e)
            null
        }

        if (idToken != null) {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    // Create/update user document in Firestore
                    createUserDocument(user, user.displayName ?: "Anonymous")
                    _authState.value = AuthState.Authenticated(user)
                    _loginError.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase auth with Google credential failed", e)
                _loginError.value = e.localizedMessage ?: "Authentication with Google failed"
            }
        } else {
            _loginError.value = "Failed to retrieve valid Google ID token"
        }
        _isLoading.value = false
    }

    private suspend fun createUserDocument(user: FirebaseUser, displayName: String) {
        try {
            val userDoc = db.collection("users").document(user.uid).get().await()
            if (!userDoc.exists()) {
                val userData = hashMapOf(
                    "userId" to user.uid,
                    "displayName" to displayName,
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "totalHabits" to 0,
                    "currentStreak" to 0,
                    "bestStreak" to 0,
                    "totalCompletions" to 0,
                    "joinedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(user.uid).set(userData).await()

                // Also create leaderboard entry
                val leaderboardData = hashMapOf(
                    "userId" to user.uid,
                    "displayName" to displayName,
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "totalCompletions" to 0,
                    "currentStreak" to 0,
                    "bestStreak" to 0,
                    "score" to 0,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("leaderboard").document(user.uid).set(leaderboardData).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user document", e)
        }
    }

    fun switchToRegister() {
        _authState.value = AuthState.NeedsRegistration
        _loginError.value = null
    }

    fun switchToLogin() {
        _authState.value = AuthState.NeedsLogin
        _loginError.value = null
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.NeedsLogin
    }

    fun clearError() {
        _loginError.value = null
    }

    fun setError(message: String) {
        _loginError.value = message
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun checkIsOnboardingComplete(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getBoolean("onboardingComplete") ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markOnboardingComplete() {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid).update("onboardingComplete", true).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update onboarding status", e)
        }
    }

    suspend fun updateDisplayName(newName: String) {
        val user = auth.currentUser ?: return
        try {
            val profileUpdates = userProfileChangeRequest {
                displayName = newName
            }
            user.updateProfile(profileUpdates).await()
            db.collection("users").document(user.uid).update("displayName", newName).await()
            db.collection("leaderboard").document(user.uid).update("displayName", newName).await()
            
            _authState.value = AuthState.Authenticated(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update display name", e)
        }
    }
}
