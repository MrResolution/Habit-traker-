package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.example.ui.AuthState
import com.example.ui.AuthViewModel
import com.example.ui.screens.AuthScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val authViewModel: AuthViewModel = viewModel()
                val habitViewModel: HabitViewModel = viewModel()
                
                val authState by authViewModel.authState.collectAsState()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (authState) {
                            is AuthState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            is AuthState.NeedsRegistration -> {
                                AuthScreen(viewModel = authViewModel, isRegistration = true)
                            }
                            is AuthState.NeedsLogin -> {
                                AuthScreen(viewModel = authViewModel, isRegistration = false)
                            }
                            is AuthState.Authenticated -> {
                                MainDashboard(
                                    viewModel = habitViewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onLogout = { authViewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
