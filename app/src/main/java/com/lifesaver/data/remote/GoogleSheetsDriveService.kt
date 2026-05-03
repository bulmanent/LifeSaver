package com.lifesaver.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lifesaver.auth.GoogleAuthManager
import com.lifesaver.data.preferences.AppPreferences
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class GoogleSheetsDriveService(
    private val context: Context,
    private val authManager: GoogleAuthManager,
    private val preferences: AppPreferences
) {

    private val appContext = context.applicationContext
    private val transport = AndroidHttp.newCompatibleTransport()
    private val gson = Gson()

    suspend fun fetchAll(): List<DocumentGroup> = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val groupRows = readRows(GROUPS_SHEET, GROUP_COLUMNS.size)
        val pageRows = readRows(PAGES_SHEET, PAGES_COLUMNS.size)

        val pagesByGroup = pageRows
            .mapNotNull { it.toPageOrNull() }
            .groupBy { it.groupId }

        groupRows
            .mapNotNull { row -> row.toGroupOrNull(pagesByGroup[row.valueAt(0)].orEmpty()) }
            .sortedBy { it.sequence }
    }

    suspend fun addGroup(title: String, tags: List<String>, description: String?): DocumentGroup =
        withContext(Dispatchers.IO) {
            ensureReady()
            ensureSheetsStructure()

            val sequence = fetchAll().maxOfOrNull { it.sequence }?.plus(1) ?: 0
            val group = DocumentGroup(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                sequence = sequence,
                tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                driveFolderId = null,
                pages = emptyList()
            )
            appendRow(GROUPS_SHEET, group.toRow())
            group
        }

    suspend fun updateGroup(group: DocumentGroup) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val groupRow = findRowById(GROUPS_SHEET, group.id, GROUP_COLUMNS.size)
            ?: error("Group not found")

        val folderId = group.driveFolderId?.takeIf { it.isNotBlank() }
        if (folderId != null) {
            updateDriveFileMetadata(
                fileId = folderId,
                metadata = mapOf("name" to sanitizeFolderName(group.title))
            )
        }

        updateRow(
            sheetName = GROUPS_SHEET,
            rowNumber = groupRow.rowNumber,
            values = group.toRow()
        )
    }

    suspend fun deleteGroup(group: DocumentGroup) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val pageRows = readRows(PAGES_SHEET, PAGES_COLUMNS.size)
            .filter { it.valueAt(1) == group.id }
            .sortedByDescending { it.rowNumber }

        for (row in pageRows) {
            row.toPageOrNull()?.driveFileId?.takeIf { !it.isNullOrBlank() }?.let { deleteDriveFile(it) }
            deleteRow(PAGES_SHEET, row.rowNumber)
        }

        val groupRow = findRowById(GROUPS_SHEET, group.id, GROUP_COLUMNS.size)
        if (groupRow != null) {
            deleteRow(GROUPS_SHEET, groupRow.rowNumber)
        }

        group.driveFolderId?.takeIf { it.isNotBlank() }?.let { deleteDriveFolderRecursively(it) }
    }

    suspend fun reorderGroups(groups: List<DocumentGroup>) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val rowsById = readRows(GROUPS_SHEET, GROUP_COLUMNS.size).associateBy { it.valueAt(0) }
        groups.forEachIndexed { index, group ->
            val row = rowsById[group.id] ?: return@forEachIndexed
            updateRow(GROUPS_SHEET, row.rowNumber, row.values.padToSize(GROUP_COLUMNS.size).updated(2, index.toString()))
        }
    }

    suspend fun addPage(groupId: String, localUri: Uri, caption: String?, sequence: Int? = null): DocumentPage =
        withContext(Dispatchers.IO) {
            ensureReady()
            ensureSheetsStructure()

            val groups = fetchAll()
            val group = groups.firstOrNull { it.id == groupId } ?: error("Group not found")
            val folderId = ensureGroupFolder(group)

            val upload = uploadDriveFile(
                folderId = folderId,
                groupTitle = group.title,
                sourceUri = localUri
            )

            if (group.driveFolderId != folderId) {
                val groupRow = findRowById(GROUPS_SHEET, group.id, GROUP_COLUMNS.size)
                    ?: error("Group row not found")
                updateRow(
                    GROUPS_SHEET,
                    groupRow.rowNumber,
                    group.copy(driveFolderId = folderId).toRow()
                )
            }

            val nextSequence = groups.firstOrNull { it.id == groupId }?.pages?.maxOfOrNull { it.sequence }?.plus(1) ?: 0
            val page = DocumentPage(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                sequence = sequence ?: nextSequence,
                driveFileId = upload.id,
                caption = caption?.trim()?.takeIf { it.isNotEmpty() },
                itemType = itemTypeForMimeType(upload.mimeType),
                textContent = null,
                mimeType = upload.mimeType,
                fileName = upload.name
            )
            appendRow(PAGES_SHEET, page.toRow())
            page
        }

    suspend fun addTextPage(groupId: String, textContent: String, caption: String?, sequence: Int? = null): DocumentPage =
        withContext(Dispatchers.IO) {
            ensureReady()
            ensureSheetsStructure()

            val groups = fetchAll()
            val group = groups.firstOrNull { it.id == groupId } ?: error("Group not found")
            val nextSequence = group.pages.maxOfOrNull { it.sequence }?.plus(1) ?: 0

            val page = DocumentPage(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                sequence = sequence ?: nextSequence,
                driveFileId = null,
                caption = caption?.trim()?.takeIf { it.isNotEmpty() },
                itemType = DocumentPage.TYPE_TEXT,
                textContent = textContent.trim().takeIf { it.isNotEmpty() },
                mimeType = "text/plain",
                fileName = null
            )
            appendRow(PAGES_SHEET, page.toRow())
            page
        }

    suspend fun deletePage(page: DocumentPage) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        page.driveFileId?.takeIf { it.isNotBlank() }?.let { deleteDriveFile(it) }
        val row = findRowById(PAGES_SHEET, page.id, PAGES_COLUMNS.size) ?: return@withContext
        deleteRow(PAGES_SHEET, row.rowNumber)
    }

    suspend fun updatePage(page: DocumentPage) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val row = findRowById(PAGES_SHEET, page.id, PAGES_COLUMNS.size) ?: error("Page not found")
        updateRow(PAGES_SHEET, row.rowNumber, page.toRow())
    }

    suspend fun reorderPages(groupId: String, pages: List<DocumentPage>) = withContext(Dispatchers.IO) {
        ensureReady()
        ensureSheetsStructure()

        val rowsById = readRows(PAGES_SHEET, PAGES_COLUMNS.size)
            .filter { it.valueAt(1) == groupId }
            .associateBy { it.valueAt(0) }

        pages.forEachIndexed { index, page ->
            val row = rowsById[page.id] ?: return@forEachIndexed
            updateRow(PAGES_SHEET, row.rowNumber, row.values.padToSize(PAGES_COLUMNS.size).updated(2, index.toString()))
        }
    }

    fun openDriveFileStream(fileId: String): InputStream {
        ensureReadySync()
        require(fileId.isNotBlank()) { "Missing Drive file ID" }
        val response = executeWithRetry {
            requestFactory()
                .buildGetRequest(GenericUrl("${DRIVE_FILES_URL}/${fileId}?alt=media"))
                .execute()
        }
        val bytes = response.content.readBytes()
        response.disconnect()
        return ByteArrayInputStream(bytes)
    }

    suspend fun prepareDriveFileForViewing(page: DocumentPage): Uri = withContext(Dispatchers.IO) {
        ensureReadySync()
        val fileId = page.driveFileId?.takeIf { it.isNotBlank() } ?: error("Missing Drive file ID")
        val fileBytes = openDriveFileStream(fileId).use { it.readBytes() }
        val downloadsDir = java.io.File(appContext.cacheDir, "viewer").apply { mkdirs() }
        val fileName = sanitizeFileName(
            page.fileName
                ?: buildUploadFileName("LifeSaver", null, page.mimeType ?: DEFAULT_FILE_MIME_TYPE)
        )
        val file = java.io.File(downloadsDir, fileName)
        file.writeBytes(fileBytes)
        FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    private fun ensureReadySync() {
        check(preferences.isBackendConfigured()) { "Sheets ID and Drive folder ID must be configured" }
        check(authManager.isSignedInWithRequiredScopes()) { "Google account not connected" }
    }

    private fun ensureReady() {
        ensureReadySync()
    }

    private fun ensureSheetsStructure() {
        val existingSheets = fetchSheetProperties()
        val missingSheets = listOf(GROUPS_SHEET, PAGES_SHEET).filterNot { existingSheets.containsKey(it) }
        if (missingSheets.isNotEmpty()) {
            val requests = JsonArray()
            missingSheets.forEach { name ->
                val request = JsonObject()
                val addSheet = JsonObject()
                val properties = JsonObject()
                properties.addProperty("title", name)
                addSheet.add("properties", properties)
                request.add("addSheet", addSheet)
                requests.add(request)
            }
            postJson(
                url = "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}:batchUpdate",
                body = mapOf("requests" to requests)
            )
        }

        writeHeaderIfNeeded(GROUPS_SHEET, GROUP_COLUMNS)
        writeHeaderIfNeeded(PAGES_SHEET, PAGES_COLUMNS)
    }

    private fun fetchSheetProperties(): Map<String, Int> {
        val response = getJsonObject(
            "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}?fields=sheets(properties(sheetId,title))"
        )
        val sheets = response.getAsJsonArray("sheets") ?: JsonArray()
        return sheets.associate { element ->
            val properties = element.asJsonObject.getAsJsonObject("properties")
            properties.get("title").asString to properties.get("sheetId").asInt
        }
    }

    private fun writeHeaderIfNeeded(sheetName: String, headers: List<String>) {
        val range = "$sheetName!A1:${columnName(headers.size)}1"
        val existing = readRange(range)
        val current = existing.firstOrNull()
        if (current == null || current != headers) {
            updateRange(range, listOf(headers))
        }
    }

    private fun readRows(sheetName: String, columnCount: Int): List<SheetRow> {
        val range = "$sheetName!A2:${columnName(columnCount)}"
        val rows = readRange(range)
        return rows.mapIndexed { index, values ->
            SheetRow(rowNumber = index + 2, values = values)
        }
    }

    private fun readRange(range: String): List<List<String>> {
        val response = getJsonObject(
            "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}/values/$range"
        )
        val values = response.getAsJsonArray("values") ?: return emptyList()
        return values.map { row ->
            row.asJsonArray.map { cell -> cell.asString }
        }
    }

    private fun appendRow(sheetName: String, values: List<String>) {
        postJson(
            url = "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}/values/$sheetName!A1:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS",
            body = mapOf("values" to listOf(values))
        )
    }

    private fun updateRow(sheetName: String, rowNumber: Int, values: List<String>) {
        val range = "$sheetName!A$rowNumber:${columnName(values.size)}$rowNumber"
        updateRange(range, listOf(values))
    }

    private fun updateRange(range: String, values: List<List<String>>) {
        putJson(
            url = "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}/values/$range?valueInputOption=RAW",
            body = mapOf("values" to values)
        )
    }

    private fun deleteRow(sheetName: String, rowNumber: Int) {
        val sheetId = fetchSheetProperties()[sheetName] ?: error("Missing sheet $sheetName")
        val deleteDimension = mapOf(
            "range" to mapOf(
                "sheetId" to sheetId,
                "dimension" to "ROWS",
                "startIndex" to rowNumber - 1,
                "endIndex" to rowNumber
            )
        )
        postJson(
            url = "https://sheets.googleapis.com/v4/spreadsheets/${preferences.sheetsId}:batchUpdate",
            body = mapOf("requests" to listOf(mapOf("deleteDimension" to deleteDimension)))
        )
    }

    private fun findRowById(sheetName: String, id: String, columnCount: Int): SheetRow? {
        return readRows(sheetName, columnCount).firstOrNull { it.valueAt(0) == id }
    }

    private fun ensureGroupFolder(group: DocumentGroup): String {
        group.driveFolderId?.takeIf { it.isNotBlank() }?.let { return it }
        val created = createDriveFolder(
            name = sanitizeFolderName(group.title),
            parentFolderId = preferences.rootFolderId ?: error("Missing Drive folder ID")
        )
        return created.id
    }

    private fun createDriveFolder(name: String, parentFolderId: String): DriveFileMetadata {
        val response = postJson(
            url = "$DRIVE_FILES_URL?fields=id,name,mimeType",
            body = mapOf(
                "name" to name,
                "mimeType" to DRIVE_FOLDER_MIME_TYPE,
                "parents" to listOf(parentFolderId)
            )
        )
        return response.toDriveFileMetadata()
    }

    private fun updateDriveFileMetadata(fileId: String, metadata: Map<String, Any>) {
        patchJson("$DRIVE_FILES_URL/$fileId", metadata)
    }

    private fun deleteDriveFile(fileId: String) {
        try {
            executeWithRetry {
                requestFactory().buildDeleteRequest(GenericUrl("$DRIVE_FILES_URL/$fileId")).execute()
            }.disconnect()
        } catch (_: HttpResponseException) {
        }
    }

    private fun deleteDriveFolderRecursively(folderId: String) {
        listDriveChildren(folderId).forEach { child ->
            if (child.mimeType == DRIVE_FOLDER_MIME_TYPE) {
                deleteDriveFolderRecursively(child.id)
            } else {
                deleteDriveFile(child.id)
            }
        }
        deleteDriveFile(folderId)
    }

    private fun listDriveChildren(folderId: String): List<DriveFileMetadata> {
        val queryUrl = "$DRIVE_FILES_URL?q='${folderId}'%20in%20parents%20and%20trashed%3Dfalse&fields=files(id,name,mimeType)"
        val response = getJsonObject(queryUrl)
        val files = response.getAsJsonArray("files") ?: JsonArray()
        return files.map { it.asJsonObject.toDriveFileMetadata() }
    }

    private fun uploadDriveFile(folderId: String, groupTitle: String, sourceUri: Uri): DriveFileMetadata {
        val fileBytes = openUriBytes(sourceUri)
        val mimeType = resolveMimeType(sourceUri)
        val originalName = queryDisplayName(sourceUri)
        val fileName = buildUploadFileName(groupTitle, originalName, mimeType)

        val metadataJson = gson.toJson(
            mapOf(
                "name" to fileName,
                "parents" to listOf(folderId)
            )
        )

        val boundary = "LifeSaverBoundary${System.currentTimeMillis()}"
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: ").append(mimeType).append("\r\n\r\n")
        }.toByteArray() + fileBytes + "\r\n--$boundary--\r\n".toByteArray()

        val response = executeWithRetry {
            requestFactory()
                .buildPostRequest(
                    GenericUrl("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType"),
                    ByteArrayContent("multipart/related; boundary=$boundary", body)
                )
                .execute()
        }
        val responseBody = response.parseAsString()
        response.disconnect()

        return JsonParser.parseString(responseBody).asJsonObject.toDriveFileMetadata()
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).name }
        }
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }

    private fun openUriBytes(uri: Uri): ByteArray {
        if (uri.scheme == "file") {
            val file = uri.path?.let { File(it) } ?: error("Unable to read selected file")
            return file.readBytes()
        }
        return appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read selected file")
    }

    private fun resolveMimeType(uri: Uri): String {
        if (uri.scheme == "file") {
            val extension = uri.path
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
            return extension
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                ?: DEFAULT_FILE_MIME_TYPE
        }
        return appContext.contentResolver.getType(uri) ?: DEFAULT_FILE_MIME_TYPE
    }

    private fun requestFactory(): HttpRequestFactory {
        val credential = authManager.requireCredential()
        val initializer = HttpRequestInitializer { request: HttpRequest ->
            credential.initialize(request)
            request.setConnectTimeout(30_000)
            request.setReadTimeout(30_000)
        }
        return transport.createRequestFactory(initializer)
    }

    private fun getJsonObject(url: String): JsonObject {
        val httpResponse = executeWithRetry {
            requestFactory()
                .buildGetRequest(GenericUrl(url))
                .execute()
        }
        val response = httpResponse.parseAsString()
        httpResponse.disconnect()
        return JsonParser.parseString(response).asJsonObject
    }

    private fun postJson(url: String, body: Any): JsonObject {
        val httpResponse = executeWithRetry {
            requestFactory()
                .buildPostRequest(GenericUrl(url), ByteArrayContent.fromString("application/json; charset=UTF-8", gson.toJson(body)))
                .execute()
        }
        val response = httpResponse.parseAsString()
        httpResponse.disconnect()
        return if (response.isBlank()) JsonObject() else JsonParser.parseString(response).asJsonObject
    }

    private fun putJson(url: String, body: Any): JsonObject {
        val httpResponse = executeWithRetry {
            requestFactory()
                .buildPutRequest(GenericUrl(url), ByteArrayContent.fromString("application/json; charset=UTF-8", gson.toJson(body)))
                .execute()
        }
        val response = httpResponse.parseAsString()
        httpResponse.disconnect()
        return if (response.isBlank()) JsonObject() else JsonParser.parseString(response).asJsonObject
    }

    private fun patchJson(url: String, body: Any): JsonObject {
        val request = requestFactory()
            .buildPatchRequest(GenericUrl(url), ByteArrayContent.fromString("application/json; charset=UTF-8", gson.toJson(body)))
        val httpResponse = executeWithRetry { request.execute() }
        val response = httpResponse.parseAsString()
        httpResponse.disconnect()
        return if (response.isBlank()) JsonObject() else JsonParser.parseString(response).asJsonObject
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

    private fun SheetRow.toGroupOrNull(pages: List<DocumentPage>): DocumentGroup? {
        val id = valueAt(0).ifBlank { return null }
        return DocumentGroup(
            id = id,
            title = valueAt(1),
            sequence = valueAt(2).toIntOrNull() ?: 0,
            tags = valueAt(3).split(",").map { it.trim() }.filter { it.isNotEmpty() },
            description = valueAt(4).ifBlank { null },
            driveFolderId = valueAt(5).ifBlank { null },
            pages = pages.sortedBy { it.sequence }
        )
    }

    private fun SheetRow.toPageOrNull(): DocumentPage? {
        val id = valueAt(0).ifBlank { return null }
        return DocumentPage(
            id = id,
            groupId = valueAt(1),
            sequence = valueAt(2).toIntOrNull() ?: 0,
            driveFileId = valueAt(3).ifBlank { null },
            caption = valueAt(4).ifBlank { null },
            itemType = valueAt(5).ifBlank { DocumentPage.TYPE_IMAGE },
            textContent = valueAt(6).ifBlank { null },
            mimeType = valueAt(7).ifBlank { null },
            fileName = valueAt(8).ifBlank { null }
        )
    }

    private fun DocumentGroup.toRow(): List<String> = listOf(
        id,
        title,
        sequence.toString(),
        tags.joinToString(","),
        description.orEmpty(),
        driveFolderId.orEmpty()
    )

    private fun DocumentPage.toRow(): List<String> = listOf(
        id,
        groupId,
        sequence.toString(),
        driveFileId.orEmpty(),
        caption.orEmpty(),
        itemType,
        textContent.orEmpty(),
        mimeType.orEmpty(),
        fileName.orEmpty()
    )

    private fun JsonObject.toDriveFileMetadata(): DriveFileMetadata =
        DriveFileMetadata(
            id = get("id").asString,
            name = get("name")?.asString,
            mimeType = get("mimeType")?.asString
        )

    private fun buildUploadFileName(groupTitle: String, originalName: String?, mimeType: String): String {
        val extension = originalName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: mimeType.substringAfter('/', "bin").substringBefore(';')
        return "${sanitizeFolderName(groupTitle)}_${System.currentTimeMillis()}.$extension"
    }

    private fun itemTypeForMimeType(mimeType: String?): String {
        return if (mimeType?.startsWith("image/") == true) {
            DocumentPage.TYPE_IMAGE
        } else {
            DocumentPage.TYPE_FILE
        }
    }

    private fun sanitizeFolderName(raw: String): String {
        return raw.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "LifeSaver" }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "lifesaver_file" }
    }

    private fun columnName(columnCount: Int): String {
        var value = columnCount
        val builder = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            builder.append(('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return builder.reverse().toString()
    }

    private data class SheetRow(
        val rowNumber: Int,
        val values: List<String>
    ) {
        fun valueAt(index: Int): String = values.getOrElse(index) { "" }
    }

    private data class DriveFileMetadata(
        val id: String,
        val name: String?,
        val mimeType: String?
    )

    companion object {
        private const val GROUPS_SHEET = "Groups"
        private const val PAGES_SHEET = "Pages"
        private val GROUP_COLUMNS = listOf("id", "title", "sequence", "tags", "description", "driveFolderId")
        private val PAGES_COLUMNS = listOf("id", "groupId", "sequence", "driveFileId", "caption", "itemType", "textContent", "mimeType", "fileName")
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val DEFAULT_FILE_MIME_TYPE = "application/octet-stream"
    }
}

private fun List<String>.padToSize(size: Int): MutableList<String> =
    toMutableList().apply {
        while (this.size < size) add("")
    }

private fun MutableList<String>.updated(index: Int, value: String): List<String> =
    apply { this[index] = value }
