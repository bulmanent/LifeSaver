package com.lifesaver.model

data class DocumentPage(
    val id: String,
    val groupId: String,
    val sequence: Int,
    val uri: String,
    val caption: String?
)
