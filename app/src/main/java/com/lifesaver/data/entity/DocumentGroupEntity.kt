package com.lifesaver.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_groups")
data class DocumentGroupEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sequence: Int,
    val tags: String, // comma-separated
    val description: String?
)
