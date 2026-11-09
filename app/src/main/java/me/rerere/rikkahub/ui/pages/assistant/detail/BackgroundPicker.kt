package me.rerere.rikkahub.ui.pages.assistant.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Image02
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.BACKGROUND_ROTATION_INTERVALS
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun BackgroundPicker(
    modifier: Modifier = Modifier,
    backgrounds: List<String>,
    backgroundOpacity: Float = 1.0f,
    rotationEnabled: Boolean = false,
    rotationIntervalSeconds: Int = 30,
    onBackgroundsUpdate: (List<String>) -> Unit,
    onRotationEnabledChange: (Boolean) -> Unit,
    onRotationIntervalChange: (Int) -> Unit,
) {
    val filesManager: FilesManager = koinInject()
    var showPickOption by remember { mutableStateOf(false) }
    var showUrlInput by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val imported = filesManager.createChatFilesByContents(uris).map(Uri::toString)
            onBackgroundsUpdate((backgrounds + imported).distinct())
        }
    }

    val previewOpacity = backgroundOpacity.coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (backgrounds.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    items = backgrounds,
                    key = { _, url -> url },
                ) { index, background ->
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.large)
                            .alpha(previewOpacity),
                    ) {
                        AsyncImage(
                            model = background,
                            contentDescription = stringResource(
                                R.string.assistant_page_background_image_number,
                                index + 1,
                            ),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        IconButton(
                            onClick = {
                                onBackgroundsUpdate(backgrounds.filterIndexed { i, _ -> i != index })
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                        ) {
                            Icon(
                                imageVector = HugeIcons.Cancel01,
                                contentDescription = stringResource(
                                    R.string.assistant_page_remove_background_image
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { showPickOption = true }) {
                Icon(
                    imageVector = HugeIcons.Image02,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(
                        if (backgrounds.isEmpty()) {
                            R.string.assistant_page_select_background
                        } else {
                            R.string.assistant_page_add_background_images
                        }
                    )
                )
            }
            if (backgrounds.isNotEmpty()) {
                TextButton(onClick = { onBackgroundsUpdate(emptyList()) }) {
                    Text(stringResource(R.string.assistant_page_clear_background_images))
                }
            }
        }

        if (backgrounds.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.assistant_page_background_rotation),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.assistant_page_background_rotation_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = rotationEnabled,
                    onCheckedChange = onRotationEnabledChange,
                )
            }

            val normalizedInterval = rotationIntervalSeconds
                .takeIf { it in BACKGROUND_ROTATION_INTERVALS }
                ?: 30
            val selectedIndex = BACKGROUND_ROTATION_INTERVALS.indexOf(normalizedInterval)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.assistant_page_background_rotation_interval),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = rotationIntervalLabel(normalizedInterval),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { value ->
                        onRotationIntervalChange(
                            BACKGROUND_ROTATION_INTERVALS[value.roundToInt()]
                        )
                    },
                    valueRange = 0f..BACKGROUND_ROTATION_INTERVALS.lastIndex.toFloat(),
                    steps = BACKGROUND_ROTATION_INTERVALS.size - 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showPickOption) {
        AlertDialog(
            onDismissRequest = {
                showPickOption = false
            },
            title = {
                Text(stringResource(R.string.assistant_page_select_background))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPickOption = false
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.assistant_page_select_from_gallery))
                    }
                    Button(
                        onClick = {
                            showPickOption = false
                            urlInput = ""
                            showUrlInput = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.assistant_page_enter_image_url))
                    }
                    if (backgrounds.isNotEmpty()) {
                        Button(
                            onClick = {
                                showPickOption = false
                                onBackgroundsUpdate(emptyList())
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.assistant_page_remove_background))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickOption = false }) {
                    Text(stringResource(R.string.assistant_page_cancel))
                }
            },
        )
    }

    if (showUrlInput) {
        AlertDialog(
            onDismissRequest = {
                showUrlInput = false
            },
            title = {
                Text(stringResource(R.string.assistant_page_enter_image_url))
            },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(stringResource(R.string.assistant_page_image_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/image.jpg") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = urlInput.trim()
                        if (url.isNotEmpty()) {
                            onBackgroundsUpdate((backgrounds + url).distinct())
                            showUrlInput = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.assistant_page_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInput = false }) {
                    Text(stringResource(R.string.assistant_page_cancel))
                }
            },
        )
    }
}

@Composable
private fun rotationIntervalLabel(seconds: Int): String =
    if (seconds < 60) {
        stringResource(R.string.assistant_page_background_rotation_seconds, seconds)
    } else {
        stringResource(R.string.assistant_page_background_rotation_minutes, seconds / 60)
    }
