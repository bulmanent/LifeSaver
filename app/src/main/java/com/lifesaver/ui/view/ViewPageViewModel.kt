package com.lifesaver.ui.view

import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentPage

class ViewPageViewModel(
    private val repository: DocumentRepository,
    private val groupId: String
) : ViewModel() {

    val pages: LiveData<List<DocumentPage>> =
        repository.getPagesForGroup(groupId).asLiveData()

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

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
