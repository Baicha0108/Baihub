package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Fullscreen
import me.rerere.rikkahub.R

@Composable
internal fun FullScreenContentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    fullscreenTitle: String,
    fullscreenDescription: String,
    fullscreenPlaceholder: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp),
    minLines: Int = 5,
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenContent by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxSize(),
            minLines = minLines,
        )
        IconButton(
            onClick = {
                fullscreenContent = value
                showFullscreen = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = HugeIcons.Fullscreen,
                contentDescription = fullscreenDescription,
            )
        }
    }

    if (showFullscreen) {
        FullScreenContentEditor(
            initial = fullscreenContent,
            title = fullscreenTitle,
            placeholder = fullscreenPlaceholder,
            onDismiss = { showFullscreen = false },
            onConfirm = {
                onValueChange(it)
                showFullscreen = false
            },
        )
    }
}

@Composable
internal fun FullScreenContentEditor(
    initial: String,
    title: String,
    placeholder: String,
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
                        text = title,
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
                    placeholder = { Text(placeholder) },
                )
            }
        }
    }
}
