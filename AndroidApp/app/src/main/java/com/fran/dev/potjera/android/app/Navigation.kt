package com.fran.dev.potjera.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fran.dev.potjera.android.app.domain.models.user.User
import com.fran.dev.potjera.android.app.home.presentation.HomeScreen
import com.fran.dev.potjera.android.app.profile.presentation.ProfileScreen
import com.fran.dev.potjera.android.app.ui.LoadingErrorScreen
import com.fran.dev.potjera.android.app.ui.LoadingScreen
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Profile

@Composable
fun Navigation(modifier: Modifier) {
    val navController = rememberNavController()
    val sharedViewModel: SharedViewModel = viewModel()
    val user by sharedViewModel.user.collectAsStateWithLifecycle()
    val isLoading by sharedViewModel.isLoading.collectAsStateWithLifecycle()

    when {
        isLoading -> LoadingScreen()
        user == null -> LoadingErrorScreen(onRetry = { sharedViewModel.loadUser(true) })
        else -> NavHost(navController = navController, startDestination = Home) {
            composable<Home> {
                HomeScreen(modifier = modifier, user = user!!, onProfile = {
                    navController.navigate(Profile)
                })
            }
            composable<Profile> {
                ProfileScreen(user = user!!) {
                    navController.popBackStack()
                }
            }
        }
    }
}