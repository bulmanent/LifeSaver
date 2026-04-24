package com.lifesaver.ui.detail

import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModel() {

    val group: LiveData<DocumentGroup?> = liveData {
        emit(repository.getGroupById(groupId))
    }

    val pages: LiveData<List<DocumentPage>> =
        repository.getPagesForGroup(groupId).asLiveData()

    fun deletePage(page: DocumentPage) {
        viewModelScope.launch {
            repository.deletePage(page)
        }
    }

    fun reorderPages(pages: List<DocumentPage>) {
        viewModelScope.launch {
            repository.reorderPages(pages)
        }
    }

    fun addPage(uri: String, caption: String?) {
        viewModelScope.launch {
            repository.addPage(groupId, uri, caption)
        }
    }
}

class GroupDetailViewModelFactory(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GroupDetailViewModel(repository, groupId) as T
    }
}
