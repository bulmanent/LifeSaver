package com.lifesaver.ui.addedit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import kotlinx.coroutines.launch

class AddEditGroupViewModel(
    private val repository: DocumentRepository,
    private val groupId: String?
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    val existingGroup: LiveData<DocumentGroup?> =
        repository.allGroups
            .map { groups -> groups.firstOrNull { it.id == groupId } }
            .asLiveData()

    fun saveGroup(title: String, tags: List<String>, description: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val existing = groupId?.let { repository.getGroupById(it) }
                if (existing != null) {
                    repository.updateGroup(existing.copy(title = title, tags = tags, description = description))
                } else {
                    repository.addGroup(title, tags, description)
                }
                onDone()
            }.onFailure {
                _errorMessage.value = it.message ?: "Unable to save group"
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
