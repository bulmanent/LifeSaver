package com.lifesaver.model

data class GroupSummary(
    val id: String,
    val title: String,
    val sequence: Int,
    val tags: List<String>,
    val description: String?,
    val pageCount: Int
)
