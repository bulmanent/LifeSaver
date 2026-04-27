package com.lifesaver.model

data class DocumentPage(
    val id: String,
    val groupId: String,
    val sequence: Int,
    val driveFileId: String? = null,
    val caption: String?,
    val itemType: String = TYPE_IMAGE,
    val textContent: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null
) {
    val isTextOnly: Boolean
        get() = itemType == TYPE_TEXT

    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_TEXT = "text"
    }
}
