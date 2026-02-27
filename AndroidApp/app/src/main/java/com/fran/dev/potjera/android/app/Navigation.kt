package com.fran.dev.potjera.android.app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fran.dev.potjera.android.app.auth.presentation.AuthScreen
import com.fran.dev.potjera.android.app.game.presentation.GameRoute
import com.fran.dev.potjera.android.app.home.presentation.HomeScreen
import com.fran.dev.potjera.android.app.profile.presentation.ProfileScreen
import com.fran.dev.potjera.android.app.room.presentation.create.CreateRoomScreen
import com.fran.dev.potjera.android.app.room.presentation.join.JoinRoomScreen
import com.fran.dev.potjera.android.app.room.presentation.lobby.RoomLobbyScreen
import com.fran.dev.potjera.android.app.ui.LoadingScreen
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Profile

@Serializable
object CreateRoom

@Serializable
object JoinRoom

@Serializable
data class Lobby(val roomId: String)

@Serializable
data class Game(val gameSessionId: String)

@Composable
fun Navigation(modifier: Modifier) {

    val TAG = "Navigation"

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
                }, onCreateRoom = {
                    navController.navigate(CreateRoom)
                }, onJoinRoom = {
                    navController.navigate(JoinRoom)
                })
            }
            composable<Profile> {
                ProfileScreen(user = user!!, onBack = {
                    navController.popBackStack()
                }, onLogout = {
                    mainViewModel.setUser(null)
                })
            }
            composable<CreateRoom> {
                CreateRoomScreen(
                    user = user!!,
                    onBack = {
                        navController.popBackStack()
                    },
                    onCreateRoom = { roomId ->
                        //TODO: navigate to lobby
                        Log.d(TAG, "Navigation: Room Created")
                        navController.navigate(Lobby(roomId))
                    }
                )
            }
            composable<JoinRoom> {
                JoinRoomScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLobby = { roomId ->
                        navController.navigate(Lobby(roomId))
                    }
                )
            }
            composable<Lobby> { backStackEntry ->
                val lobby: Lobby = backStackEntry.toRoute()
                RoomLobbyScreen(
                    roomId = lobby.roomId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onStartGame = { gameSessionId ->
                        navController.navigate(Game(gameSessionId))
                    }
                )
            }
            composable<Game> { backStackEntry ->
                val game: Game = backStackEntry.toRoute()
                GameRoute(
                    gameSessionId  = game.gameSessionId,
                    onNavigateHome = {
                        navController.navigate(Home) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
