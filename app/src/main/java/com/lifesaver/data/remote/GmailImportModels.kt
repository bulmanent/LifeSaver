package com.lifesaver.data.remote

data class GmailAttachmentSummary(
    val messageId: String,
    val attachmentId: String?,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Int,
    val inlineData: String?,
    val subject: String?
)

data class GmailMessageSummary(
    val id: String,
    val subject: String,
    val from: String,
    val dateText: String,
    val attachments: List<GmailAttachmentSummary>
)
