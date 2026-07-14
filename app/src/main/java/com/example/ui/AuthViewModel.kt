package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object NeedsRegistration : AuthState()
    object NeedsLogin : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val count = userDao.getUserCount()
            if (count == 0) {
                _authState.value = AuthState.NeedsRegistration
            } else {
                _authState.value = AuthState.NeedsLogin
            }
        }
    }

    fun register(username: String, pin: String) {
        if (username.isBlank() || pin.isBlank()) {
            _loginError.value = "Username and password cannot be empty"
            return
        }
        viewModelScope.launch {
            val count = userDao.getUserCount()
            if (count > 0) {
                _loginError.value = "An account already exists. Please login."
                return@launch
            }
            val newUser = User(username = username, pin = pin)
            userDao.insertUser(newUser)
            _authState.value = AuthState.Authenticated(newUser)
            _loginError.value = null
        }
    }

    fun login(username: String, pin: String) {
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user != null && user.pin == pin) {
                _authState.value = AuthState.Authenticated(user)
                _loginError.value = null
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.NeedsLogin
    }
    
    fun clearError() {
        _loginError.value = null
    }
}
