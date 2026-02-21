package com.fran.dev.potjera.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fran.dev.potjera.android.app.auth.presentation.AuthScreen
import com.fran.dev.potjera.android.app.home.presentation.HomeScreen
import com.fran.dev.potjera.android.app.profile.presentation.ProfileScreen
import com.fran.dev.potjera.android.app.ui.LoadingScreen
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Profile

@Composable
fun Navigation(modifier: Modifier) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val user by mainViewModel.user.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()

    when {
        isLoading -> LoadingScreen()
        user == null -> AuthScreen(onSignInSuccess = { user ->
            mainViewModel.setUser(
                user
            )
        })

        else -> NavHost(navController = navController, startDestination = Home) {
            composable<Home> {
                HomeScreen(modifier = modifier, user = user!!, onProfile = {
                    navController.navigate(Profile)
                })
            }
            composable<Profile> {
                ProfileScreen(user = user!!, onBack = {
                    navController.popBackStack()
                }, onLogout = {
                    mainViewModel.setUser(null)
                })
            }
        }
    }
}
