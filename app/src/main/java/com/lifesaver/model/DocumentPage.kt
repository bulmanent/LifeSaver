package com.lifesaver.model

data class DocumentPage(
    val id: String,
    val groupId: String,
    val sequence: Int,
    val driveFileId: String,
    val caption: String?,
    val mimeType: String? = null,
    val fileName: String? = null
)
