package com.lifesaver.data.repository

import com.lifesaver.data.dao.DocumentGroupDao
import com.lifesaver.data.dao.DocumentPageDao
import com.lifesaver.data.entity.DocumentGroupEntity
import com.lifesaver.data.entity.DocumentPageEntity
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage
import com.lifesaver.model.GroupSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DocumentRepository(
    private val groupDao: DocumentGroupDao,
    private val pageDao: DocumentPageDao
) {

    val allGroups: Flow<List<DocumentGroup>> = groupDao.getAllGroups().map { entities ->
        entities.map { it.toDomain() }
    }

    val allGroupSummaries: Flow<List<GroupSummary>> =
        groupDao.getAllGroupsWithCount().map { list ->
            list.map { gwc ->
                GroupSummary(
                    id = gwc.group.id,
                    title = gwc.group.title,
                    sequence = gwc.group.sequence,
                    tags = if (gwc.group.tags.isBlank()) emptyList()
                           else gwc.group.tags.split(",").map { it.trim() },
                    description = gwc.group.description,
                    pageCount = gwc.pageCount
                )
            }
        }

    fun getPagesForGroup(groupId: String): Flow<List<DocumentPage>> =
        pageDao.getPagesForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getPageCountForGroup(groupId: String): Flow<Int> =
        pageDao.getPageCountForGroup(groupId)

    suspend fun getGroupById(id: String): DocumentGroup? =
        groupDao.getGroupById(id)?.toDomain()

    suspend fun getPageById(id: String): DocumentPage? =
        pageDao.getPageById(id)?.toDomain()

    suspend fun getPagesForGroupOnce(groupId: String): List<DocumentPage> =
        pageDao.getPagesForGroupOnce(groupId).map { it.toDomain() }

    suspend fun addGroup(title: String, tags: List<String>, description: String?): DocumentGroup {
        val maxSeq = groupDao.getMaxSequence() ?: -1
        val entity = DocumentGroupEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            sequence = maxSeq + 1,
            tags = tags.joinToString(","),
            description = description
        )
        groupDao.insertGroup(entity)
        return entity.toDomain()
    }

    suspend fun updateGroup(group: DocumentGroup) {
        groupDao.updateGroup(group.toEntity())
    }

    suspend fun deleteGroup(group: DocumentGroup) {
        groupDao.deleteGroup(group.toEntity())
    }

    suspend fun reorderGroups(groups: List<DocumentGroup>) {
        val updated = groups.mapIndexed { index, group ->
            group.copy(sequence = index).toEntity()
        }
        groupDao.insertGroups(updated)
    }

    suspend fun addPage(groupId: String, uri: String, caption: String?): DocumentPage {
        val maxSeq = pageDao.getMaxSequenceForGroup(groupId) ?: -1
        val entity = DocumentPageEntity(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            sequence = maxSeq + 1,
            uri = uri,
            caption = caption
        )
        pageDao.insertPage(entity)
        return entity.toDomain()
    }

    suspend fun updatePage(page: DocumentPage) {
        pageDao.updatePage(page.toEntity())
    }

    suspend fun deletePage(page: DocumentPage) {
        pageDao.deletePage(page.toEntity())
    }

    suspend fun reorderPages(pages: List<DocumentPage>) {
        val updated = pages.mapIndexed { index, page ->
            page.copy(sequence = index).toEntity()
        }
        pageDao.insertPages(updated)
    }

    suspend fun replaceAll(groups: List<DocumentGroup>) {
        pageDao.deleteAllPages()
        groupDao.deleteAllGroups()
        val groupEntities = groups.map { it.toEntity() }
        groupDao.insertGroups(groupEntities)
        val pageEntities = groups.flatMap { g -> g.pages.map { it.toEntity() } }
        pageDao.insertPages(pageEntities)
    }

    suspend fun mergeAll(groups: List<DocumentGroup>) {
        val groupEntities = groups.map { it.toEntity() }
        groupDao.insertGroups(groupEntities)
        val pageEntities = groups.flatMap { g -> g.pages.map { it.toEntity() } }
        pageDao.insertPages(pageEntities)
    }

    suspend fun exportAll(): List<DocumentGroup> {
        val groupEntities = groupDao.getAllGroupsOnce()
        return groupEntities.map { ge ->
            val pages = pageDao.getPagesForGroupOnce(ge.id).map { it.toDomain() }
            ge.toDomain().copy(pages = pages)
        }
    }
}

private fun DocumentGroupEntity.toDomain() = DocumentGroup(
    id = id,
    title = title,
    sequence = sequence,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    description = description,
    pages = emptyList()
)

private fun DocumentPageEntity.toDomain() = DocumentPage(
    id = id,
    groupId = groupId,
    sequence = sequence,
    uri = uri,
    caption = caption
)

private fun DocumentGroup.toEntity() = DocumentGroupEntity(
    id = id,
    title = title,
    sequence = sequence,
    tags = tags.joinToString(","),
    description = description
)

private fun DocumentPage.toEntity() = DocumentPageEntity(
    id = id,
    groupId = groupId,
    sequence = sequence,
    uri = uri,
    caption = caption
)
