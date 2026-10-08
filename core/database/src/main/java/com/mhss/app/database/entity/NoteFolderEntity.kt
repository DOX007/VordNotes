package com.mhss.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mhss.app.domain.model.NoteFolder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "note_folders")
@Serializable
data class NoteFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    // Ny kolumn för föräldramapp
    val parentId: Int? = null
)

fun NoteFolderEntity.toNoteFolder(): NoteFolder {
    return NoteFolder(
        name = name,
        id = id,
        parentId = parentId
    )
}

fun NoteFolder.toNoteFolderEntity(): NoteFolderEntity {
    return NoteFolderEntity(
        id = id,
        name = name,
        parentId = parentId
    )
}

fun List<NoteFolderEntity>.withoutIds() = map { it.copy(id = 0) }
