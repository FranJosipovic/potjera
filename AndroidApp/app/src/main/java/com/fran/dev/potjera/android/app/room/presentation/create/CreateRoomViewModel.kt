package com.fran.dev.potjera.android.app.room.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.room.repository.CreateRoomResult
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val repository: RoomRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun createRoom(isPrivate: Boolean, onRoomCreated: (roomId: String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.createRoom(isPrivate)) {
                is CreateRoomResult.Success<*> -> {
                    onRoomCreated(result.data!!.roomId)
                }

                is CreateRoomResult.UnknownError<*> -> {
                    //TODO: Notify error
                }
            }

            _isLoading.value = false
        }
    }
}