package com.lifesaver.ui.addedit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AddEditGroupViewModel(
    private val repository: DocumentRepository,
    private val groupId: String?
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving

    val existingGroup: LiveData<DocumentGroup?> =
        repository.allGroups
            .map { groups -> groups.firstOrNull { it.id == groupId } }
            .asLiveData()

    fun saveGroup(title: String, tags: List<String>, description: String?, onDone: (DocumentGroup) -> Unit) {
        // Guard against double-submission: while a save is in flight (Sheets
        // can be slow), ignore further taps. Without this a second tap
        // appends a duplicate/"ghost" group row. Set/checked on the main
        // thread, so re-entrant clicks can't slip past.
        if (_saving.value == true) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val existing = groupId?.let { repository.getGroupById(it) }
                val saved = if (existing != null) {
                    val updated = existing.copy(title = title, tags = tags, description = description)
                    repository.updateGroup(updated)
                    updated
                } else {
                    repository.addGroup(title, tags, description)
                }
                onDone(saved)
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Unable to save group"
            } finally {
                _saving.value = false
            }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}

class AddEditGroupViewModelFactory(
    private val repository: DocumentRepository,
    private val groupId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AddEditGroupViewModel(repository, groupId) as T
    }
}
