package com.lifesaver.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.GroupSummary
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    private val _errorMessage = MutableLiveData<String?>()
    private val _isLoading = MutableLiveData(false)

    val searchQuery: LiveData<String> = _searchQuery
    val errorMessage: LiveData<String?> = _errorMessage
    val isLoading: LiveData<Boolean> = _isLoading

    val filteredGroups: LiveData<List<GroupSummary>> =
        repository.allGroupSummaries.asLiveData().switchMap { summaries ->
            _searchQuery.map { query ->
                if (query.isBlank()) {
                    summaries
                } else {
                    val lower = query.lowercase()
                    summaries.filter { summary ->
                        summary.title.lowercase().contains(lower) ||
                            summary.tags.any { it.lowercase().contains(lower) }
                    }
                }
            }
        }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun needsSetup(): Boolean = repository.needsSetup()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.refresh() }
                .onFailure { _errorMessage.value = it.message ?: "Unable to sync with Google Sheets" }
            _isLoading.value = false
        }
    }

    fun deleteSummary(summary: GroupSummary) {
        viewModelScope.launch {
            runCatching {
                val group = repository.getGroupById(summary.id) ?: return@runCatching
                repository.deleteGroup(group)
            }.onFailure {
                _errorMessage.value = it.message ?: "Unable to delete group"
            }
        }
    }

    fun reorderSummaries(summaries: List<GroupSummary>) {
        viewModelScope.launch {
            runCatching {
                val groups = summaries.mapIndexed { index, summary ->
                    DocumentGroup(
                        id = summary.id,
                        title = summary.title,
                        sequence = index,
                        tags = summary.tags,
                        description = summary.description
                    )
                }
                repository.reorderGroups(groups)
            }.onFailure {
                _errorMessage.value = it.message ?: "Unable to reorder groups"
            }
        }
    }

    fun consumeError() {
        _errorMessage.value = null
    }
}

class HomeViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
