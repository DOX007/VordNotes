package com.mhss.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val title: String = "",
    val content: String = "",
    val createdDate: Long = 0L,
    val updatedDate: Long = 0L,
    val pinned: Boolean = false,
    val folderId: Int? = null,
    val orderIndex: Int = 0,
    val id: Int = 0,
    // Id:n på valda vård-pins (se NotePin i core/ui). Separat från den röda "viktig"-pinnen (pinned).
    val pins: List<String> = emptyList(),
)
