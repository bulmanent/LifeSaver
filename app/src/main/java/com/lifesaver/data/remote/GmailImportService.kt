package com.lifesaver.data.remote

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lifesaver.auth.GoogleAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class GmailImportService(
    context: Context,
    private val authManager: GoogleAuthManager
) {

    private val appContext = context.applicationContext
    private val transport = AndroidHttp.newCompatibleTransport()

    suspend fun searchMessagesWithAttachments(subjectTerm: String, limit: Int = 25): List<GmailMessageSummary> =
        withContext(Dispatchers.IO) {
            ensureReady()
            try {
                val trimmedTerm = subjectTerm.trim()
                require(trimmedTerm.isNotEmpty()) { "Enter a subject search term" }
                val query = buildSearchQuery(trimmedTerm)
                val response = getJsonObject(
                    "$GMAIL_MESSAGES_URL?maxResults=$limit&q=$query&fields=messages/id"
                )
                val messages = response.getAsJsonArray("messages") ?: JsonArray()
                messages.mapNotNull { messageElement ->
                    val messageId = messageElement.asJsonObject.get("id")?.asString ?: return@mapNotNull null
                    getMessageSummary(messageId)
                }
            } catch (e: HttpResponseException) {
                throw IllegalStateException(explainHttpException(e), e)
            }
        }

    suspend fun downloadAttachmentToCache(attachment: GmailAttachmentSummary): Uri =
        withContext(Dispatchers.IO) {
            ensureReady()
            try {
                val fileBytes = when {
                    !attachment.inlineData.isNullOrBlank() -> decodeBase64Url(attachment.inlineData)
                    !attachment.attachmentId.isNullOrBlank() -> {
                        val response = getJsonObject(
                            "$GMAIL_MESSAGES_URL/${attachment.messageId}/attachments/${attachment.attachmentId}"
                        )
                        decodeBase64Url(response.get("data")?.asString ?: error("Missing attachment data"))
                    }
                    else -> error("Attachment data unavailable")
                }

                val cacheDir = File(appContext.cacheDir, "gmail_imports").apply { mkdirs() }
                val file = File(cacheDir, sanitizeFileName(attachment.fileName))
                file.writeBytes(fileBytes)
                Uri.fromFile(file)
            } catch (e: HttpResponseException) {
                throw IllegalStateException(explainHttpException(e), e)
            }
        }

    private fun getMessageSummary(messageId: String): GmailMessageSummary? {
        val fields = "id,internalDate,payload(headers(name,value),body/attachmentId,body/data,body/size,filename,mimeType,parts)"
        val response = getJsonObject("$GMAIL_MESSAGES_URL/$messageId?format=full&fields=$fields")
        val payload = response.getAsJsonObject("payload") ?: return null
        val attachments = collectAttachments(
            messageId = messageId,
            part = payload
        )
        if (attachments.isEmpty()) return null

        val headers = payload.getAsJsonArray("headers") ?: JsonArray()
        val subject = headerValue(headers, "Subject").ifBlank { "No subject" }
        val from = headerValue(headers, "From").ifBlank { "Unknown sender" }
        val dateText = response.get("internalDate")?.asString?.toLongOrNull()?.let(::formatDate)
            ?: headerValue(headers, "Date").ifBlank { "" }

        return GmailMessageSummary(
            id = messageId,
            subject = subject,
            from = from,
            dateText = dateText,
            attachments = attachments.map { it.copy(subject = subject) }
        )
    }

    private fun collectAttachments(messageId: String, part: JsonObject): List<GmailAttachmentSummary> {
        val attachments = mutableListOf<GmailAttachmentSummary>()
        val fileName = part.get("filename")?.asString.orEmpty()
        val body = part.getAsJsonObject("body")
        val attachmentId = body?.get("attachmentId")?.asString
        val inlineData = body?.get("data")?.asString
        val sizeBytes = body?.get("size")?.asInt ?: 0
        val mimeType = part.get("mimeType")?.asString

        if (fileName.isNotBlank() && (!attachmentId.isNullOrBlank() || !inlineData.isNullOrBlank())) {
            attachments += GmailAttachmentSummary(
                messageId = messageId,
                attachmentId = attachmentId,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                inlineData = inlineData,
                subject = null
            )
        }

        val parts = part.getAsJsonArray("parts") ?: return attachments
        parts.forEach { child ->
            attachments += collectAttachments(messageId, child.asJsonObject)
        }
        return attachments
    }

    private fun formatDate(timestampMs: Long): String {
        return DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault()
        ).format(Date(timestampMs))
    }

    private fun headerValue(headers: JsonArray, name: String): String {
        return headers.firstOrNull { header ->
            header.asJsonObject.get("name")?.asString.equals(name, ignoreCase = true)
        }?.asJsonObject?.get("value")?.asString.orEmpty()
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "gmail_attachment" }
    }

    private fun buildSearchQuery(subjectTerm: String): String {
        val rawQuery = """has:attachment subject:"$subjectTerm""""
        return URLEncoder.encode(rawQuery, Charsets.UTF_8.name())
    }

    private fun decodeBase64Url(data: String): ByteArray {
        return Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun ensureReady() {
        check(authManager.hasGmailReadOnlyAccess()) { "Gmail access has not been granted" }
    }

    private fun requestFactory(): HttpRequestFactory {
        val credential = authManager.requireCredential(listOf(GoogleAuthManager.GMAIL_READONLY_SCOPE))
        val initializer = HttpRequestInitializer { request: HttpRequest ->
            credential.initialize(request)
            request.setConnectTimeout(30_000)
            request.setReadTimeout(30_000)
        }
        return transport.createRequestFactory(initializer)
    }

    private fun getJsonObject(url: String): JsonObject {
        val response = executeWithRetry {
            requestFactory().buildGetRequest(GenericUrl(url)).execute()
        }
        val body = response.parseAsString()
        response.disconnect()
        return JsonParser.parseString(body).asJsonObject
    }

    private fun explainHttpException(error: HttpResponseException): String {
        val details = runCatching {
            val body = error.content ?: return@runCatching null
            val root = JsonParser.parseString(body).asJsonObject
            val errorObject = root.getAsJsonObject("error") ?: return@runCatching null
            val message = errorObject.get("message")?.asString
            val status = errorObject.get("status")?.asString
            listOfNotNull(message, status).joinToString(" ")
        }.getOrNull().orEmpty()

        val message = if (details.isNotBlank()) details else error.statusMessage.orEmpty()

        return when {
            error.statusCode == 403 && message.contains("has not been used", ignoreCase = true) ->
                "Gmail API is not enabled in the Google Cloud project."
            error.statusCode == 403 && message.contains("accessNotConfigured", ignoreCase = true) ->
                "Gmail API is not enabled in the Google Cloud project."
            error.statusCode == 403 && message.contains("insufficient", ignoreCase = true) ->
                "Gmail permission was granted, but the Gmail scope is not usable for this app configuration."
            error.statusCode == 403 && message.isNotBlank() ->
                "Gmail request failed: $message"
            else ->
                "Gmail request failed: HTTP ${error.statusCode}${if (message.isNotBlank()) " - $message" else ""}"
        }
    }

    private fun <T> executeWithRetry(block: () -> T): T {
        var delayMs = 500L
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: HttpResponseException) {
                val retryable = e.statusCode == 429 || e.statusCode in 500..599
                if (!retryable || attempt == 2) throw e
                Thread.sleep(delayMs)
                delayMs *= 2
            }
        }
        error("Unreachable retry state")
    }

    companion object {
        private const val GMAIL_MESSAGES_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages"
    }
}
