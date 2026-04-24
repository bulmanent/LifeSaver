package com.lifesaver.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lifesaver.model.DocumentGroup
import com.lifesaver.model.DocumentPage

data class ExportData(
    val version: Int = 1,
    val groups: List<GroupExport>
)

data class GroupExport(
    val id: String,
    val title: String,
    val sequence: Int,
    val tags: List<String>,
    val description: String?,
    val driveFolderId: String?,
    val pages: List<PageExport>
)

data class PageExport(
    val id: String,
    val groupId: String,
    val sequence: Int,
    val driveFileId: String,
    val caption: String?,
    val mimeType: String?,
    val fileName: String?
)

object JsonManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(groups: List<DocumentGroup>): String {
        val exports = groups.map { group ->
            GroupExport(
                id = group.id,
                title = group.title,
                sequence = group.sequence,
                tags = group.tags,
                description = group.description,
                driveFolderId = group.driveFolderId,
                pages = group.pages.map { page ->
                    PageExport(
                        id = page.id,
                        groupId = page.groupId,
                        sequence = page.sequence,
                        driveFileId = page.driveFileId,
                        caption = page.caption,
                        mimeType = page.mimeType,
                        fileName = page.fileName
                    )
                }
            )
        }
        return gson.toJson(ExportData(groups = exports))
    }

    fun fromJson(json: String): List<DocumentGroup> {
        val type = object : TypeToken<ExportData>() {}.type
        val exportData: ExportData = gson.fromJson(json, type)
        return exportData.groups.map { ge ->
            DocumentGroup(
                id = ge.id,
                title = ge.title,
                sequence = ge.sequence,
                tags = ge.tags,
                description = ge.description,
                driveFolderId = ge.driveFolderId,
                pages = ge.pages.map { pe ->
                    DocumentPage(
                        id = pe.id,
                        groupId = pe.groupId,
                        sequence = pe.sequence,
                        driveFileId = pe.driveFileId,
                        caption = pe.caption,
                        mimeType = pe.mimeType,
                        fileName = pe.fileName
                    )
                }
            )
        }
    }
}
