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
    val pages: List<PageExport>
)

data class PageExport(
    val id: String,
    val groupId: String,
    val sequence: Int,
    val uri: String,
    val caption: String?
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
                pages = group.pages.map { page ->
                    PageExport(
                        id = page.id,
                        groupId = page.groupId,
                        sequence = page.sequence,
                        uri = page.uri,
                        caption = page.caption
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
                pages = ge.pages.map { pe ->
                    DocumentPage(
                        id = pe.id,
                        groupId = pe.groupId,
                        sequence = pe.sequence,
                        uri = pe.uri,
                        caption = pe.caption
                    )
                }
            )
        }
    }
}
