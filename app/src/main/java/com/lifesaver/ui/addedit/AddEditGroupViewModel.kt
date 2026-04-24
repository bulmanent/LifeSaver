package com.lifesaver.ui.addedit

import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import kotlinx.coroutines.launch

class AddEditGroupViewModel(
    private val repository: DocumentRepository,
    private val groupId: String?
) : ViewModel() {

    val existingGroup: LiveData<DocumentGroup?> = liveData {
        emit(groupId?.let { repository.getGroupById(it) })
    }

    fun saveGroup(title: String, tags: List<String>, description: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val existing = groupId?.let { repository.getGroupById(it) }
            if (existing != null) {
                repository.updateGroup(existing.copy(title = title, tags = tags, description = description))
            } else {
                repository.addGroup(title, tags, description)
            }
            onDone()
        }
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
