package com.fran.dev.potjera.android.app.game.gamefinished

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.models.GameFinishPlayerResult
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class GameFinishedViewModel @Inject constructor(
    repository: GameSessionRepository
) : ViewModel() {

    private val _results = MutableStateFlow<List<GameFinishPlayerResult>>(emptyList())
    val results: StateFlow<List<GameFinishPlayerResult>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                if (event is GameSessionSocketEvent.GameFinishedEvent) {
                    _results.value = event.results.map {
                        GameFinishPlayerResult(
                            playerId = it.playerId,
                            correctAnswers = it.correctAnswers
                        )
                    }
                }
            }
        }
    }
}