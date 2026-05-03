package com.lifesaver.ui.view

import android.net.Uri
import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentPage
import kotlinx.coroutines.launch

class ViewPageViewModel(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModel() {

    val pages: LiveData<List<DocumentPage>> =
        repository.getPagesForGroup(groupId).asLiveData()

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    private val _openFileUri = MutableLiveData<Uri?>()
    val openFileUri: LiveData<Uri?> = _openFileUri

    val currentPage: LiveData<DocumentPage?> = MediatorLiveData<DocumentPage?>().also { mediator ->
        fun update() {
            mediator.value = pages.value?.getOrNull(_currentIndex.value ?: 0)
        }
        mediator.addSource(pages) { update() }
        mediator.addSource(_currentIndex) { update() }
    }

    fun setIndex(index: Int) {
        _currentIndex.value = index
    }

    fun goNext() {
        val size = pages.value?.size ?: 0
        val cur = _currentIndex.value ?: 0
        if (cur + 1 < size) _currentIndex.value = cur + 1
    }

    fun goPrev() {
        val cur = _currentIndex.value ?: 0
        if (cur > 0) _currentIndex.value = cur - 1
    }

    fun updatePage(page: DocumentPage) {
        viewModelScope.launch {
            runCatching { repository.updatePage(page) }
                .onFailure { _errorMessage.value = it.message ?: "Unable to update item" }
        }
    }

    fun openCurrentFile() {
        val page = currentPage.value ?: return
        if (!page.isFile) return
        viewModelScope.launch {
            runCatching { repository.prepareDriveFileForViewing(page) }
                .onSuccess { _openFileUri.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Unable to open file" }
        }
    }

    fun consumeOpenFileUri() {
        _openFileUri.value = null
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}

class ViewPageViewModelFactory(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ViewPageViewModel(repository, groupId) as T
    }
}
