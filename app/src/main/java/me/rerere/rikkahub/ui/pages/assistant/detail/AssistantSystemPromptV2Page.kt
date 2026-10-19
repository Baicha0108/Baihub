package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Download01
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.Fullscreen
import me.rerere.hugeicons.stroke.Message02
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.export.CustomPromptMergeResult
import me.rerere.rikkahub.data.export.buildCustomPromptExport
import me.rerere.rikkahub.data.export.mergeCustomPromptEntries
import me.rerere.rikkahub.data.export.parseCustomPromptExport
import me.rerere.rikkahub.data.model.SystemPromptEntry
import me.rerere.rikkahub.ui.components.ai.ExtensionEmptyState
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon
import me.rerere.rikkahub.ui.components.ui.ItemAction
import me.rerere.rikkahub.ui.components.ui.ItemActionMenu
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.longPressReorder
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.writeClipboardText
import me.rerere.ui.components.FormItem
import me.rerere.ui.components.RikkaConfirmDialog
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.uuid.Uuid

@Composable
fun AssistantSystemPromptV2Page(id: String) {
    val vm: AssistantSystemPromptV2VM = koinViewModel(
        parameters = { parametersOf(id) }
    )
    val entries by vm.entries.collectAsStateWithLifecycle()
    val assistantName by vm.assistantName.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Uuid>>(emptySet()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_page_system_prompt_v2)) },
                navigationIcon = { BackButton() },
                actions = {
                    if (selectionMode) {
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.isEmpty()) {
                                    entries.orEmpty().map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.assistant_system_prompt_v2_select_all))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        AssistantSystemPromptV2Content(
            innerPadding = innerPadding,
            entries = entries,
            assistantName = assistantName,
            selectionMode = selectionMode,
            selectedIds = selectedIds,
            onStartSelection = {
                selectionMode = true
                selectedIds = emptySet()
            },
            onCancelSelection = {
                selectionMode = false
                selectedIds = emptySet()
            },
            onToggleSelection = { entryId ->
                selectedIds = if (entryId in selectedIds) {
                    selectedIds - entryId
                } else {
                    selectedIds + entryId
                }
            },
            onUpsert = vm::upsert,
            onDelete = vm::delete,
            onToggle = vm::setEnabled,
            onSetEntries = vm::setEntries,
            onMove = vm::move,
        )
    }
}

@Composable
private fun AssistantSystemPromptV2Content(
    innerPadding: PaddingValues,
    entries: List<SystemPromptEntry>?,
    assistantName: String,
    selectionMode: Boolean,
    selectedIds: Set<Uuid>,
    onStartSelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onToggleSelection: (Uuid) -> Unit,
    onUpsert: (SystemPromptEntry) -> Unit,
    onDelete: (SystemPromptEntry) -> Unit,
    onToggle: (SystemPromptEntry, Boolean) -> Unit,
    onSetEntries: (List<SystemPromptEntry>) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val unnamed = stringResource(R.string.assistant_system_prompt_v2_unnamed)
    val unnamedAssistant = stringResource(R.string.assistant_system_prompt_v2_unnamed_assistant)
    val exportSuccess = stringResource(R.string.assistant_system_prompt_v2_export_success)
    val exportFailed = stringResource(R.string.assistant_system_prompt_v2_export_failed)
    val importFailed = stringResource(R.string.assistant_system_prompt_v2_import_failed)
    val importEmpty = stringResource(R.string.assistant_system_prompt_v2_import_empty)
    val importSuccess = stringResource(R.string.export_import_success)
    val copied = stringResource(R.string.copied)

    var editing by remember { mutableStateOf<SystemPromptEntry?>(null) }
    var deleting by remember { mutableStateOf<SystemPromptEntry?>(null) }
    var pendingImport by remember { mutableStateOf<CustomPromptMergeResult?>(null) }
    var exportEntries by remember { mutableStateOf<List<SystemPromptEntry>>(emptyList()) }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = JsonInstantPretty.encodeToString(
                        buildCustomPromptExport(exportEntries, unnamed)
                    )
                    context.contentResolver.openOutputStream(uri)
                        ?.bufferedWriter()
                        ?.use { it.write(json) }
                        ?: error("Cannot open output stream")
                }
            }
            result.onSuccess {
                toaster.show(exportSuccess, ToastType.Success)
                onCancelSelection()
            }.onFailure {
                toaster.show(exportFailed, ToastType.Error)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Cannot read input file")
                    parseCustomPromptExport(text)
                }
            }
            imported.onSuccess { file ->
                if (file.entries.isEmpty()) {
                    toaster.show(importEmpty, ToastType.Warning)
                } else {
                    pendingImport = mergeCustomPromptEntries(
                        existing = entries.orEmpty(),
                        imported = file.entries,
                        unnamedTitle = unnamed,
                    )
                }
            }.onFailure {
                toaster.show(importFailed, ToastType.Error)
            }
        }
    }

    val exportFileName = remember(assistantName, unnamedAssistant) {
        val name = assistantName.trim().ifBlank { unnamedAssistant }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
        "$name.json"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            entries == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            entries.isEmpty() -> {
                ExtensionEmptyState(
                    message = stringResource(R.string.assistant_system_prompt_v2_empty),
                    icon = HugeIcons.Message02,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding + PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = if (selectionMode) 112.dp else 128.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = lazyListState,
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ReorderableItem(
                            state = reorderableState,
                            key = entry.id,
                        ) { isDragging ->
                            SystemPromptEntryCard(
                                entry = entry,
                                modifier = longPressReorder(
                                    isDragging = isDragging,
                                    enabled = !selectionMode,
                                ),
                                selectionMode = selectionMode,
                                selected = entry.id in selectedIds,
                                onClick = {
                                    if (selectionMode) {
                                        onToggleSelection(entry.id)
                                    } else {
                                        editing = entry
                                    }
                                },
                                onToggle = { onToggle(entry, it) },
                                onCopy = {
                                    context.writeClipboardText(entry.content)
                                    toaster.show(copied, ToastType.Success)
                                },
                                onDelete = { deleting = entry },
                            )
                        }
                    }
                }
            }
        }

        if (selectionMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancelSelection) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        exportEntries = entries.orEmpty().filter { it.id in selectedIds }
                        exportLauncher.launch(exportFileName)
                    },
                    enabled = selectedIds.isNotEmpty(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(88.dp),
                ) {
                    Text(stringResource(R.string.assistant_system_prompt_v2_export))
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onStartSelection,
                    enabled = !entries.isNullOrEmpty(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(HugeIcons.Download01, null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.assistant_system_prompt_v2_export))
                }
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    enabled = entries != null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(HugeIcons.FileImport, null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.assistant_system_prompt_v2_import))
                }
                Button(
                    onClick = { editing = SystemPromptEntry(title = unnamed) },
                    enabled = entries != null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(HugeIcons.Add01, null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.assistant_system_prompt_v2_add))
                }
            }
        }
    }

    editing?.let { entry ->
        SystemPromptEntryEditSheet(
            entry = entry,
            unnamedTitle = unnamed,
            onDismiss = { editing = null },
            onSave = {
                onUpsert(it)
                editing = null
            },
        )
    }

    deleting?.let { entry ->
        RikkaConfirmDialog(
            show = true,
            title = stringResource(R.string.confirm_delete),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                onDelete(entry)
                deleting = null
            },
            onDismiss = { deleting = null },
        ) {
            Text(
                stringResource(
                    R.string.common_delete_confirm_message,
                    entry.title.ifBlank { unnamed },
                )
            )
        }
    }

    pendingImport?.let { merge ->
        RikkaConfirmDialog(
            show = true,
            title = stringResource(R.string.assistant_system_prompt_v2_import_confirm_title),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                onSetEntries(merge.entries)
                pendingImport = null
                toaster.show(importSuccess, ToastType.Success)
            },
            onDismiss = { pendingImport = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        R.string.assistant_system_prompt_v2_import_confirm_message,
                        merge.replacedTitles.size,
                        merge.appendedTitles.size,
                    )
                )
                if (merge.replacedTitles.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.assistant_system_prompt_v2_import_replace_label
                        ) + "\n" + merge.replacedTitles.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (merge.appendedTitles.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.assistant_system_prompt_v2_import_append_label
                        ) + "\n" + merge.appendedTitles.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemPromptEntryCard(
    entry: SystemPromptEntry,
    modifier: Modifier = Modifier,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val unnamed = stringResource(R.string.assistant_system_prompt_v2_unnamed)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CustomColors.listItemColors.containerColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                )
            }
            CardGroupIcon(
                icon = HugeIcons.Message02,
                containerColor = if (entry.enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (entry.enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.title.ifBlank { unnamed },
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.content.isNotBlank()) {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!entry.enabled) {
                    Tag(type = TagType.WARNING) {
                        Text(stringResource(R.string.prompt_page_disabled))
                    }
                }
            }
            if (!selectionMode) {
                Switch(
                    checked = entry.enabled,
                    onCheckedChange = onToggle,
                )
                ItemActionMenu(
                    actions = listOf(
                        ItemAction(
                            text = stringResource(R.string.assistant_system_prompt_v2_copy),
                            icon = HugeIcons.Copy01,
                            onClick = onCopy,
                        ),
                        ItemAction(
                            text = stringResource(R.string.delete),
                            icon = HugeIcons.Delete01,
                            destructive = true,
                            onClick = onDelete,
                        ),
                    )
                )
            }
        }
    }
}

@Composable
private fun SystemPromptEntryEditSheet(
    entry: SystemPromptEntry,
    unnamedTitle: String,
    onDismiss: () -> Unit,
    onSave: (SystemPromptEntry) -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var draft by remember(entry.id) { mutableStateOf(entry) }
    var fullscreenContent by remember(entry.id) { mutableStateOf("") }
    var showFullscreen by remember(entry.id) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            IconButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }
            ) {
                Icon(HugeIcons.ArrowDown01, null)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_system_prompt_v2_edit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text(stringResource(R.string.assistant_system_prompt_v2_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                FormItem(
                    label = { Text(stringResource(R.string.assistant_system_prompt_v2_enabled)) },
                    tail = {
                        Switch(
                            checked = draft.enabled,
                            onCheckedChange = { draft = draft.copy(enabled = it) },
                        )
                    },
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draft.content,
                        onValueChange = { draft = draft.copy(content = it) },
                        label = { Text(stringResource(R.string.assistant_system_prompt_v2_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        minLines = 8,
                    )
                    IconButton(
                        onClick = {
                            fullscreenContent = draft.content
                            showFullscreen = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Fullscreen,
                            contentDescription = stringResource(
                                R.string.assistant_system_prompt_v2_fullscreen
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        onSave(
                            draft.copy(
                                title = draft.title.trim().ifBlank { unnamedTitle },
                            )
                        )
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }

    if (showFullscreen) {
        FullScreenContentEditor(
            initial = fullscreenContent,
            onDismiss = { showFullscreen = false },
            onConfirm = {
                draft = draft.copy(content = it)
                showFullscreen = false
            },
        )
    }
}

@Composable
private fun FullScreenContentEditor(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Text(
                        text = stringResource(R.string.assistant_system_prompt_v2_fullscreen_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onConfirm(text) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(stringResource(R.string.assistant_system_prompt_v2_content))
                    },
                )
            }
        }
    }
}
