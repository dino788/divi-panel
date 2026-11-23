package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.CloudDevice
import com.example.data.remote.CloudRemotePlaylist
import com.example.data.remote.CloudRemoteRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

data class CloudActionFeedback(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class CloudPanelViewModel(
    private val cloudRepository: CloudRemoteRepository,
    private val currentUserId: String
) : ViewModel() {

    val devicesState: StateFlow<CloudUiState<List<CloudDevice>>> =
        cloudRepository.observeDevices(currentUserId)
            .map<List<CloudDevice>, CloudUiState<List<CloudDevice>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing devices", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to load cloud devices"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val remotePlaylistsState: StateFlow<CloudUiState<List<CloudRemotePlaylist>>> =
        cloudRepository.observeRemotePlaylists(currentUserId)
            .map<List<CloudRemotePlaylist>, CloudUiState<List<CloudRemotePlaylist>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing remote playlists", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to load remote playlists"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val _feedback = MutableStateFlow(CloudActionFeedback())
    val feedback: StateFlow<CloudActionFeedback> = _feedback.asStateFlow()

    fun registerThisDevice(
        deviceMac: String,
        deviceKey: String,
        deviceName: String,
        isTv: Boolean
    ) {
        if (deviceMac.isBlank() || deviceKey.isBlank()) return
        viewModelScope.launch {
            cloudRepository.registerOrHeartbeatDevice(
                userId = currentUserId,
                deviceMac = deviceMac,
                deviceKey = deviceKey,
                deviceName = deviceName,
                isTv = isTv
            )
        }
    }

    fun pushXtreamToCloud(
        targetMac: String,
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            _feedback.value = CloudActionFeedback(isSubmitting = true)
            val result = cloudRepository.pushRemoteXtreamPlaylist(
                userId = currentUserId,
                targetMac = targetMac,
                name = name,
                serverUrl = serverUrl,
                username = username,
                password = password
            )
            result.fold(
                onSuccess = {
                    _feedback.value = CloudActionFeedback(
                        successMessage = "Playlist sent to ${if (targetMac == "ALL") "all devices" else targetMac}!"
                    )
                },
                onFailure = { error ->
                    _feedback.value = CloudActionFeedback(
                        errorMessage = error.message ?: "Failed to push playlist to cloud"
                    )
                }
            )
        }
    }

    fun pushM3uToCloud(
        targetMac: String,
        name: String,
        m3uUrl: String,
        epgUrl: String
    ) {
        viewModelScope.launch {
            _feedback.value = CloudActionFeedback(isSubmitting = true)
            val result = cloudRepository.pushRemoteM3uPlaylist(
                userId = currentUserId,
                targetMac = targetMac,
                name = name,
                m3uUrl = m3uUrl,
                epgUrl = epgUrl
            )
            result.fold(
                onSuccess = {
                    _feedback.value = CloudActionFeedback(
                        successMessage = "M3U sent to ${if (targetMac == "ALL") "all devices" else targetMac}!"
                    )
                },
                onFailure = { error ->
                    _feedback.value = CloudActionFeedback(
                        errorMessage = error.message ?: "Failed to push M3U to cloud"
                    )
                }
            )
        }
    }

    fun deleteRemotePlaylist(playlistId: String) {
        viewModelScope.launch {
            val result = cloudRepository.deleteRemotePlaylist(currentUserId, playlistId)
            result.onFailure { error ->
                _feedback.update { it.copy(errorMessage = error.message ?: "Delete failed") }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            val result = cloudRepository.removeDevice(currentUserId, deviceId)
            result.onFailure { error ->
                _feedback.update { it.copy(errorMessage = error.message ?: "Failed to remove device") }
            }
        }
    }

    fun clearFeedback() {
        _feedback.value = CloudActionFeedback()
    }

    private companion object {
        const val TAG = "CloudPanelVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
