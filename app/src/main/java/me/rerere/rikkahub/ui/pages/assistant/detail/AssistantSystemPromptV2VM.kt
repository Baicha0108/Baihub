package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.SystemPromptEntry
import me.rerere.rikkahub.data.repository.SystemPromptV2Repository
import kotlin.uuid.Uuid

class AssistantSystemPromptV2VM(
    assistantId: String,
    private val repository: SystemPromptV2Repository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val id = Uuid.parse(assistantId)
    private val _entries = MutableStateFlow<List<SystemPromptEntry>?>(null)
    val entries = _entries.asStateFlow()
    val assistantName = settingsStore.settingsFlow
        .map { settings -> settings.assistants.find { it.id == id }?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            _entries.value = repository.get(id)
        }
    }

    fun upsert(entry: SystemPromptEntry) = update { list ->
        if (list.any { it.id == entry.id }) {
            list.map { if (it.id == entry.id) entry else it }
        } else {
            list + entry
        }
    }

    fun delete(entry: SystemPromptEntry) = update { list ->
        list.filterNot { it.id == entry.id }
    }

    fun setEnabled(entry: SystemPromptEntry, enabled: Boolean) = update { list ->
        list.map { if (it.id == entry.id) it.copy(enabled = enabled) else it }
    }

    fun setEntries(entries: List<SystemPromptEntry>) = update { entries }

    fun move(from: Int, to: Int) = update { list ->
        if (from !in list.indices || to !in list.indices) {
            list
        } else {
            list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    private fun update(transform: (List<SystemPromptEntry>) -> List<SystemPromptEntry>) {
        val current = _entries.value ?: return
        val next = transform(current)
        _entries.value = next
        viewModelScope.launch {
            repository.save(id, next)
        }
    }
}
