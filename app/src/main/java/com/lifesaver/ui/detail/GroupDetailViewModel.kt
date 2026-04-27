package com.lifesaver.ui.detail

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    val group: LiveData<DocumentGroup?> =
        repository.allGroups
            .map { groups -> groups.firstOrNull { it.id == groupId } }
            .asLiveData()

    val pages: LiveData<List<DocumentPage>> =
        repository.getPagesForGroup(groupId).asLiveData()

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Unable to refresh pages" }
        }
    }

    fun deletePage(page: DocumentPage) {
        viewModelScope.launch {
            runCatching { repository.deletePage(page) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to delete page" }
        }
    }

    fun reorderPages(pages: List<DocumentPage>) {
        viewModelScope.launch {
            runCatching { repository.reorderPages(groupId, pages) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to reorder pages" }
        }
    }

    fun addPage(uri: Uri, caption: String?) {
        viewModelScope.launch {
            runCatching { repository.addPage(groupId, uri, caption) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to upload image" }
        }
    }

    fun addTextPage(textContent: String, caption: String?) {
        viewModelScope.launch {
            runCatching { repository.addTextPage(groupId, textContent, caption) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to save text entry" }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
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
