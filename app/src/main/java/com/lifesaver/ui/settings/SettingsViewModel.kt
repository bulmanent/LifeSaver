package com.lifesaver.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.repository.DocumentRepository
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data class Message(val message: String) : SettingsEvent()
}

data class SettingsUiState(
    val accountEmail: String?,
    val sheetsId: String,
    val rootFolderId: String,
    val canSync: Boolean
)

class SettingsViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _event = MutableLiveData<SettingsEvent>()
    private val _uiState = MutableLiveData(buildState())

    val event: LiveData<SettingsEvent> = _event
    val uiState: LiveData<SettingsUiState> = _uiState

    fun reloadState() {
        _uiState.value = buildState()
    }

    fun saveConfig(sheetsId: String, rootFolderId: String) {
        if (sheetsId.isBlank() || rootFolderId.isBlank()) {
            _event.value = SettingsEvent.Message("Sheets ID and Drive folder ID are required")
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.updateBackendConfig(sheetsId, rootFolderId)
                _uiState.value = buildState()
            }.onSuccess {
                _event.value = SettingsEvent.Message("Configuration saved")
            }.onFailure {
                _event.value = SettingsEvent.Message(it.message ?: "Unable to save configuration")
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            runCatching {
                repository.refresh()
                _uiState.value = buildState()
            }.onSuccess {
                _event.value = SettingsEvent.Message("Synced from Google Sheets")
            }.onFailure {
                _event.value = SettingsEvent.Message(it.message ?: "Sync failed")
            }
        }
    }

    private fun buildState(): SettingsUiState {
        return SettingsUiState(
            accountEmail = repository.currentAccountEmail(),
            sheetsId = repository.currentSheetsId(),
            rootFolderId = repository.currentRootFolderId(),
            canSync = !repository.needsSetup()
        )
    }
}

class SettingsViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(repository) as T
    }
}
