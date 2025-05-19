package com.example.dacs.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dacs.viewmodel.profile.ProfileState
import com.example.dacs.viewmodel.profile.ProfileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val email by viewModel.email.collectAsState()

    LaunchedEffect(state) {
        if (state is ProfileState.Success) {
            // Reset state after successful update
            viewModel.updateDisplayName(displayName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            ) {
                Text(
                    text = (displayName.firstOrNull() ?: email.firstOrNull())?.uppercase() ?: "",
                    color = Color.White,
                    style = TextStyle(fontSize = 50.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Spacer(modifier = Modifier.size(24.dp))
            
            OutlinedTextField(
                value = displayName,
                onValueChange = { viewModel.updateDisplayName(it) },
                label = { Text("Display Name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF334155),
                    unfocusedIndicatorColor = Color(0xFF334155),
                    focusedLabelColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.size(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text("Email", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Gray,
                    unfocusedTextColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF334155),
                    unfocusedIndicatorColor = Color(0xFF334155),
                    focusedLabelColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    disabledTextColor = Color.Gray,
                    disabledContainerColor = Color.Transparent,
                    disabledIndicatorColor = Color(0xFF334155),
                    disabledLabelColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.size(32.dp))
            
            Button(
                onClick = { viewModel.updateProfile() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155)
                ),
                enabled = state !is ProfileState.Loading
            ) {
                if (state is ProfileState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Update Profile", style = TextStyle(fontSize = 16.sp))
                }
            }

            if (state is ProfileState.Error) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = (state as ProfileState.Error).message,
                    color = Color.Red,
                    style = TextStyle(fontSize = 14.sp)
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))

            var showPasswordDialog by remember { mutableStateOf(false) }
            var newPassword by remember { mutableStateOf("") }
            
            Button(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155)
                )
            ) {
                Text("Change Password", style = TextStyle(fontSize = 16.sp))
            }

            if (showPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    title = { Text("Change Password", color = Color.White) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color(0xFF334155),
                                    unfocusedIndicatorColor = Color(0xFF334155),
                                    focusedLabelColor = Color.Gray,
                                    unfocusedLabelColor = Color.Gray,
                                    cursorColor = Color.White
                                )
                            )
                            if (newPassword.isNotEmpty() && newPassword.length < 6) {
                                Text(
                                    text = "Password must be at least 6 characters",
                                    color = Color.Red,
                                    style = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updatePassword(newPassword)
                                showPasswordDialog = false
                                newPassword = ""
                            },
                            enabled = newPassword.length >= 6
                        ) {
                            Text("Change", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showPasswordDialog = false
                                newPassword = ""
                            }
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1E293B)
                )
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            Button(
                onClick = {
                    Firebase.auth.signOut()
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155)
                )
            ) {
                Text("Sign Out", style = TextStyle(fontSize = 16.sp))
            }
        }
    }
} 