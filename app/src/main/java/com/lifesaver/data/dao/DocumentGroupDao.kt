package com.lifesaver.data.dao

import androidx.room.*
import com.lifesaver.data.entity.DocumentGroupEntity
import com.lifesaver.data.entity.GroupWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentGroupDao {

    @Query("SELECT * FROM document_groups ORDER BY sequence ASC")
    fun getAllGroups(): Flow<List<DocumentGroupEntity>>

    @Query("""
        SELECT g.*, COUNT(p.id) as pageCount
        FROM document_groups g
        LEFT JOIN document_pages p ON p.groupId = g.id
        GROUP BY g.id
        ORDER BY g.sequence ASC
    """)
    fun getAllGroupsWithCount(): Flow<List<GroupWithCount>>

    @Query("SELECT * FROM document_groups WHERE id = :id")
    suspend fun getGroupById(id: String): DocumentGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DocumentGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<DocumentGroupEntity>)

    @Update
    suspend fun updateGroup(group: DocumentGroupEntity)

    @Delete
    suspend fun deleteGroup(group: DocumentGroupEntity)

    @Query("DELETE FROM document_groups")
    suspend fun deleteAllGroups()

    @Query("SELECT MAX(sequence) FROM document_groups")
    suspend fun getMaxSequence(): Int?

    @Query("SELECT * FROM document_groups ORDER BY sequence ASC")
    suspend fun getAllGroupsOnce(): List<DocumentGroupEntity>
}
