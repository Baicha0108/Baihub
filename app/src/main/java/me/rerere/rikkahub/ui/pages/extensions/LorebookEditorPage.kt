package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.InjectionPosition
import me.rerere.rikkahub.data.model.Lorebook
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FullScreenContentField
import me.rerere.rikkahub.ui.components.ui.ItemAction
import me.rerere.rikkahub.ui.components.ui.ItemActionMenu
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.cardGroupItemShape
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.ui.components.FormItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun LorebookEditorPage(id: String?) {
    val vm: PromptVM = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    var initialized by remember(id) { mutableStateOf(false) }
    var book by remember(id) { mutableStateOf<Lorebook?>(null) }

    LaunchedEffect(settings.init, id) {
        if (!initialized && !settings.init) {
            val loaded = if (id == null) {
                Lorebook()
            } else {
                settings.lorebooks.firstOrNull { it.id.toString() == id }
            }
            if (id != null && loaded == null) {
                navController.popBackStack()
            } else {
                book = loaded
                initialized = true
            }
        }
    }

    val currentBook = book
    if (!initialized || currentBook == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val entryEditState = useEditState<PromptInjection.RegexInjection> { edited ->
        val latest = book ?: return@useEditState
        val index = latest.entries.indexOfFirst { it.id == edited.id }
        book = if (index >= 0) {
            latest.copy(
                entries = latest.entries.toMutableList().apply { set(index, edited) }
            )
        } else {
            latest.copy(entries = latest.entries + edited)
        }
    }

    fun saveBook() {
        val toSave = book ?: return
        vm.updateSettings { latest ->
            val lorebooks = latest.lorebooks.toMutableList()
            val index = lorebooks.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                lorebooks[index] = toSave
            } else {
                lorebooks.add(toSave)
            }
            latest.copy(lorebooks = lorebooks)
        }
        navController.popBackStack()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (id == null) {
                                R.string.prompt_page_new_lorebook
                            } else {
                                R.string.prompt_page_edit_lorebook
                            }
                        )
                    )
                },
                navigationIcon = { BackButton() },
                actions = {
                    TextButton(onClick = ::saveBook) {
                        Text(stringResource(R.string.prompt_page_save))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = currentBook.name,
                onValueChange = { book = book?.copy(name = it) },
                label = { Text(stringResource(R.string.prompt_page_name)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = currentBook.description,
                onValueChange = { book = book?.copy(description = it) },
                label = { Text(stringResource(R.string.prompt_page_description)) },
                modifier = Modifier.fillMaxWidth(),
            )

            FormItem(
                label = { Text(stringResource(R.string.prompt_page_enabled)) },
                tail = {
                    Switch(
                        checked = currentBook.enabled,
                        onCheckedChange = { book = book?.copy(enabled = it) },
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        R.string.prompt_page_entries_format,
                        currentBook.entries.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                FilledTonalIconButton(onClick = {
                    entryEditState.open(PromptInjection.RegexInjection())
                }) {
                    Icon(
                        HugeIcons.Add01,
                        contentDescription = stringResource(R.string.prompt_page_add_entry),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                currentBook.entries.fastForEachIndexed { index, entry ->
                    RegexInjectionEntryCard(
                        entry = entry,
                        shape = cardGroupItemShape(index, currentBook.entries.size),
                        onEdit = { entryEditState.open(entry) },
                        onDelete = {
                            book = book?.let { it.copy(entries = it.entries - entry) }
                        },
                    )
                }
            }
        }
    }

    if (entryEditState.isEditing) {
        entryEditState.currentState?.let { entry ->
            RegexInjectionEditSheet(
                entry = entry,
                onDismiss = { entryEditState.dismiss() },
                onConfirm = { entryEditState.confirm() },
                onEdit = { entryEditState.currentState = it },
            )
        }
    }
}

@Composable
private fun RegexInjectionEntryCard(
    entry: PromptInjection.RegexInjection,
    shape: Shape,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.name.ifEmpty {
                        stringResource(R.string.prompt_page_unnamed_entry)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.keywords.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.prompt_page_keywords_format,
                            entry.keywords.joinToString(", "),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!entry.enabled) {
                    Tag(type = TagType.WARNING) {
                        Text(stringResource(R.string.prompt_page_disabled))
                    }
                }
            }
            ItemActionMenu(
                actions = listOf(
                    ItemAction(
                        text = stringResource(R.string.prompt_page_delete),
                        icon = HugeIcons.Delete01,
                        destructive = true,
                        onClick = onDelete,
                    ),
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegexInjectionEditSheet(
    entry: PromptInjection.RegexInjection,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: (PromptInjection.RegexInjection) -> Unit,
) {
    var newKeyword by remember(entry.id) { mutableStateOf("") }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            IconButton(onClick = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }) {
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
                text = stringResource(R.string.prompt_page_edit_entry),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { onEdit(entry.copy(name = it)) },
                    label = { Text(stringResource(R.string.prompt_page_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                FormItem(
                    label = { Text(stringResource(R.string.prompt_page_enabled)) },
                    tail = {
                        Switch(
                            checked = entry.enabled,
                            onCheckedChange = { onEdit(entry.copy(enabled = it)) },
                        )
                    },
                )

                OutlinedTextField(
                    value = entry.priority.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { value ->
                            onEdit(entry.copy(priority = value))
                        }
                    },
                    label = { Text(stringResource(R.string.prompt_page_priority_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Text(
                    stringResource(R.string.prompt_page_injection_position),
                    style = MaterialTheme.typography.titleSmall,
                )
                InjectionPositionSelector(
                    position = entry.position,
                    onSelect = { onEdit(entry.copy(position = it)) },
                )

                AnimatedVisibility(
                    visible = entry.position == InjectionPosition.AT_DEPTH
                ) {
                    OutlinedTextField(
                        value = entry.injectDepth.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { value ->
                                onEdit(entry.copy(injectDepth = value))
                            }
                        },
                        label = { Text(stringResource(R.string.prompt_page_inject_depth)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                Text(
                    stringResource(R.string.prompt_page_keywords_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    entry.keywords.forEach { keyword ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(keyword) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        onEdit(entry.copy(keywords = entry.keywords - keyword))
                                    },
                                    modifier = Modifier.size(16.dp),
                                ) {
                                    Icon(
                                        HugeIcons.Cancel01,
                                        null,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        label = { Text(stringResource(R.string.prompt_page_new_keyword)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = {
                        if (newKeyword.isNotBlank()) {
                            onEdit(entry.copy(keywords = entry.keywords + newKeyword.trim()))
                            newKeyword = ""
                        }
                    }) {
                        Icon(HugeIcons.Add01, stringResource(R.string.prompt_page_add))
                    }
                }

                FormItem(
                    label = { Text(stringResource(R.string.prompt_page_use_regex)) },
                    tail = {
                        Switch(
                            checked = entry.useRegex,
                            onCheckedChange = { onEdit(entry.copy(useRegex = it)) },
                        )
                    },
                )

                FormItem(
                    label = { Text(stringResource(R.string.prompt_page_case_sensitive)) },
                    tail = {
                        Switch(
                            checked = entry.caseSensitive,
                            onCheckedChange = { onEdit(entry.copy(caseSensitive = it)) },
                        )
                    },
                )

                FormItem(
                    label = { Text(stringResource(R.string.prompt_page_constant_active)) },
                    description = {
                        Text(stringResource(R.string.prompt_page_constant_active_desc))
                    },
                    tail = {
                        Switch(
                            checked = entry.constantActive,
                            onCheckedChange = { onEdit(entry.copy(constantActive = it)) },
                        )
                    },
                )

                OutlinedTextField(
                    value = entry.scanDepth.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { value ->
                            onEdit(entry.copy(scanDepth = value))
                        }
                    },
                    label = { Text(stringResource(R.string.prompt_page_scan_depth)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                AnimatedVisibility(
                    visible = entry.position.usesStandaloneMessage()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.prompt_page_injection_role),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        InjectionRoleSelector(
                            role = entry.role,
                            onSelect = { onEdit(entry.copy(role = it)) },
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        )
                    }
                }

                FullScreenContentField(
                    value = entry.content,
                    onValueChange = { onEdit(entry.copy(content = it)) },
                    label = stringResource(R.string.prompt_page_injection_content),
                    fullscreenTitle = stringResource(
                        R.string.prompt_page_edit_injection_content
                    ),
                    fullscreenDescription = stringResource(
                        R.string.prompt_page_fullscreen_content
                    ),
                    fullscreenPlaceholder = stringResource(
                        R.string.prompt_page_injection_content
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.prompt_page_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = entry.keywords.isNotEmpty() || entry.constantActive,
                ) {
                    Text(stringResource(R.string.prompt_page_confirm))
                }
            }
        }
    }
}
