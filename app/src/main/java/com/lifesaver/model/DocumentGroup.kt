package com.lifesaver.model

data class DocumentGroup(
    val id: String,
    val title: String,
    val sequence: Int,
    val tags: List<String>,
    val description: String?,
    val driveFolderId: String? = null,
    val pages: List<DocumentPage> = emptyList()
)
