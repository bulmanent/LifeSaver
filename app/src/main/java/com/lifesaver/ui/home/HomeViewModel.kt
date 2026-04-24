package com.lifesaver.ui.home

import androidx.lifecycle.*
import com.lifesaver.data.repository.DocumentRepository
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.GroupSummary
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    val filteredGroups: LiveData<List<GroupSummary>> =
        repository.allGroupSummaries.asLiveData().switchMap { summaries ->
            _searchQuery.map { query ->
                if (query.isBlank()) {
                    summaries
                } else {
                    val lower = query.lowercase()
                    summaries.filter { s ->
                        s.title.lowercase().contains(lower) ||
                                s.tags.any { it.lowercase().contains(lower) }
                    }
                }
            }
        }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteSummary(summary: GroupSummary) {
        viewModelScope.launch {
            val group = repository.getGroupById(summary.id) ?: return@launch
            repository.deleteGroup(group)
        }
    }

    fun reorderSummaries(summaries: List<GroupSummary>) {
        viewModelScope.launch {
            val groups = summaries.mapIndexed { index, s ->
                DocumentGroup(id = s.id, title = s.title, sequence = index, tags = s.tags, description = s.description)
            }
            repository.reorderGroups(groups)
        }
    }
}

class HomeViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
