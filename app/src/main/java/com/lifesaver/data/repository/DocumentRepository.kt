package com.lifesaver.data.repository

import android.net.Uri
import com.lifesaver.auth.GoogleAuthManager
import com.lifesaver.data.preferences.AppPreferences
import com.lifesaver.data.remote.GoogleSheetsDriveService
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage
import com.lifesaver.model.GroupSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class DocumentRepository(
    private val authManager: GoogleAuthManager,
    private val preferences: AppPreferences,
    private val remoteService: GoogleSheetsDriveService
) {

    private val groupsState = MutableStateFlow<List<DocumentGroup>>(emptyList())

    val allGroups: Flow<List<DocumentGroup>> = groupsState

    val allGroupSummaries: Flow<List<GroupSummary>> =
        groupsState.map { groups ->
            groups
                .sortedBy { it.sequence }
                .map { group ->
                    GroupSummary(
                        id = group.id,
                        title = group.title,
                        sequence = group.sequence,
                        tags = group.tags,
                        description = group.description,
                        pageCount = group.pages.size
                    )
                }
        }

    fun getPagesForGroup(groupId: String): Flow<List<DocumentPage>> =
        groupsState.map { groups ->
            groups.firstOrNull { it.id == groupId }
                ?.pages
                ?.sortedBy { it.sequence }
                ?: emptyList()
        }

    fun isConfigured(): Boolean = preferences.isBackendConfigured()

    fun isSignedIn(): Boolean = authManager.isSignedInWithRequiredScopes()

    fun needsSetup(): Boolean = !isSignedIn() || !isConfigured()

    fun currentAccountEmail(): String? = authManager.currentEmail()

    fun currentSheetsId(): String = preferences.sheetsId.orEmpty()

    fun currentRootFolderId(): String = preferences.rootFolderId.orEmpty()

    suspend fun refresh() {
        if (needsSetup()) {
            groupsState.value = emptyList()
            return
        }
        groupsState.value = remoteService.fetchAll()
    }

    suspend fun getGroupById(id: String): DocumentGroup? {
        if (groupsState.value.isEmpty() && !needsSetup()) {
            refresh()
        }
        return groupsState.value.firstOrNull { it.id == id }
    }

    suspend fun getPageById(id: String): DocumentPage? {
        if (groupsState.value.isEmpty() && !needsSetup()) {
            refresh()
        }
        return groupsState.value
            .flatMap { it.pages }
            .firstOrNull { it.id == id }
    }

    suspend fun addGroup(title: String, tags: List<String>, description: String?): DocumentGroup {
        val group = remoteService.addGroup(title, tags, description)
        refresh()
        return group
    }

    suspend fun updateGroup(group: DocumentGroup) {
        remoteService.updateGroup(group)
        refresh()
    }

    suspend fun deleteGroup(group: DocumentGroup) {
        remoteService.deleteGroup(group)
        refresh()
    }

    suspend fun reorderGroups(groups: List<DocumentGroup>) {
        remoteService.reorderGroups(groups)
        refresh()
    }

    suspend fun addPage(groupId: String, localUri: Uri, caption: String?): DocumentPage {
        val page = remoteService.addPage(groupId, localUri, caption)
        refresh()
        return page
    }

    suspend fun deletePage(page: DocumentPage) {
        remoteService.deletePage(page)
        refresh()
    }

    suspend fun reorderPages(groupId: String, pages: List<DocumentPage>) {
        remoteService.reorderPages(groupId, pages)
        refresh()
    }

    suspend fun updateBackendConfig(sheetsId: String, rootFolderId: String) {
        preferences.saveBackendConfig(sheetsId, rootFolderId)
        refresh()
    }
}
