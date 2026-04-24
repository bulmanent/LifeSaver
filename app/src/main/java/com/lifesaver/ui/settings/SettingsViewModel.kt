package com.lifesaver.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.util.JsonManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SettingsEvent {
    data class ExportReady(val json: String) : SettingsEvent()
    data class ImportSuccess(val count: Int) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}

class SettingsViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _event = MutableLiveData<SettingsEvent>()
    val event: LiveData<SettingsEvent> = _event

    fun exportData() {
        viewModelScope.launch {
            try {
                val groups = withContext(Dispatchers.IO) { repository.exportAll() }
                val json = JsonManager.toJson(groups)
                _event.value = SettingsEvent.ExportReady(json)
            } catch (e: Exception) {
                _event.value = SettingsEvent.Error(e.message ?: "Export failed")
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                        ?: throw Exception("Cannot read file")
                }
                val groups = JsonManager.fromJson(json)
                withContext(Dispatchers.IO) {
                    if (replaceAll) {
                        repository.replaceAll(groups)
                    } else {
                        repository.mergeAll(groups)
                    }
                }
                _event.value = SettingsEvent.ImportSuccess(groups.size)
            } catch (e: Exception) {
                _event.value = SettingsEvent.Error(e.message ?: "Import failed")
            }
        }
    }
}

class SettingsViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(repository) as T
    }
}
