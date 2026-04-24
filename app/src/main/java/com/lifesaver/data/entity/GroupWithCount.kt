package com.lifesaver.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class GroupWithCount(
    @Embedded val group: DocumentGroupEntity,
    @ColumnInfo(name = "pageCount") val pageCount: Int
)
