package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class SystemPromptEntry(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val content: String = "",
    val enabled: Boolean = true,
)
