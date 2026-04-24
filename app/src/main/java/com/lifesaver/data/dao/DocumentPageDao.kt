package com.lifesaver.data.dao

import androidx.room.*
import com.lifesaver.data.entity.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentPageDao {

    @Query("SELECT * FROM document_pages WHERE groupId = :groupId ORDER BY sequence ASC")
    fun getPagesForGroup(groupId: String): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE groupId = :groupId ORDER BY sequence ASC")
    suspend fun getPagesForGroupOnce(groupId: String): List<DocumentPageEntity>

    @Query("SELECT * FROM document_pages WHERE id = :id")
    suspend fun getPageById(id: String): DocumentPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>)

    @Update
    suspend fun updatePage(page: DocumentPageEntity)

    @Delete
    suspend fun deletePage(page: DocumentPageEntity)

    @Query("DELETE FROM document_pages WHERE groupId = :groupId")
    suspend fun deletePagesForGroup(groupId: String)

    @Query("DELETE FROM document_pages")
    suspend fun deleteAllPages()

    @Query("SELECT MAX(sequence) FROM document_pages WHERE groupId = :groupId")
    suspend fun getMaxSequenceForGroup(groupId: String): Int?

    @Query("SELECT COUNT(*) FROM document_pages WHERE groupId = :groupId")
    fun getPageCountForGroup(groupId: String): Flow<Int>
}
