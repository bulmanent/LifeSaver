package com.lifesaver.ui.detail

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.remote.GmailAttachmentSummary
import com.lifesaver.data.remote.GmailMessageSummary
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
    private val _isLoading = MutableLiveData(false)
    val errorMessage: LiveData<String?> = _errorMessage
    val isLoading: LiveData<Boolean> = _isLoading

    private val _gmailMessages = MutableLiveData<List<GmailMessageSummary>?>()
    val gmailMessages: LiveData<List<GmailMessageSummary>?> = _gmailMessages

    val group: LiveData<DocumentGroup?> =
        repository.allGroups
            .map { groups -> groups.firstOrNull { it.id == groupId } }
            .asLiveData()

    val pages: LiveData<List<DocumentPage>> =
        repository.getPagesForGroup(groupId).asLiveData()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Unable to refresh pages" }
            _isLoading.value = false
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

    fun addPage(uri: Uri, caption: String?, sequence: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.addPage(groupId, uri, caption, sequence) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to upload file" }
            _isLoading.value = false
        }
    }

    fun addTextPage(textContent: String, caption: String?, sequence: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.addTextPage(groupId, textContent, caption, sequence) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to save text entry" }
            _isLoading.value = false
        }
    }

    fun hasGmailAccess(): Boolean = repository.hasGmailAccess()

    fun loadGmailMessages(subjectTerm: String) {
        viewModelScope.launch {
            runCatching { repository.searchGmailMessages(subjectTerm) }
                .onSuccess { _gmailMessages.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Unable to load Gmail messages" }
        }
    }

    fun importGmailAttachment(attachment: GmailAttachmentSummary, caption: String?, sequence: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.importGmailAttachment(groupId, attachment, caption, sequence) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to import Gmail attachment" }
            _isLoading.value = false
        }
    }

    fun consumeGmailMessages() {
        _gmailMessages.value = null
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
