package com.example.dacs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dacs.screen.auth.signin.SignInScreen
import com.example.dacs.screen.auth.signup.SignUpScreen
import com.example.dacs.screen.chat.AIChatScreen
import com.example.dacs.screen.chat.ChatScreen
import com.example.dacs.screen.home.HomeScreen
import com.example.dacs.screen.profile.ProfileScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser

        val start = if (currentUser != null) "home" else "login"
        NavHost(navController = navController, startDestination = "signin") {
            composable("signin") {
                SignInScreen(navController = navController)
            }
            composable("signup") {
                SignUpScreen(navController = navController)
            }
            composable("home") {
                HomeScreen(navController)
            }
            composable("profile") {
                ProfileScreen(navController)
            }
            composable("chat/{channelId}&{channelName}", arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                },
                navArgument("channelName") {
                    type = NavType.StringType
                }
            )) {
                val channelId = it.arguments?.getString("channelId") ?: ""
                val channelName = it.arguments?.getString("channelName") ?: ""
                ChatScreen(navController, channelId, channelName)
            }
            composable("ai_chat") {
                AIChatScreen(navController)
            }
        }
    }
}