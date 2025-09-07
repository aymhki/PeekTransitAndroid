package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aymanhki.peektransit.data.models.FolderCategory
import com.aymanhki.peektransit.utils.PeekTransitConstants

@Composable
fun MoveToFolderBottomSheet(
    onDismiss: () -> Unit,
    onMove: (targetFolderIds: List<String>) -> Unit,
    folders: List<FolderCategory>
) {
    var selectedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val availableIcons = remember { PeekTransitConstants.availableIcons }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {  }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Text(
                        text = "Move to Folder(s)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = {
                            onMove(selectedFolderIds.toList())
                        },
                        enabled = selectedFolderIds.isNotEmpty()
                    ) {
                        Text("Move")
                    }
                }

                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No folders available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            val isSelected = selectedFolderIds.contains(folder.id)
                            val isEnabled = (!selectedFolderIds.contains(PeekTransitConstants.UNCATEGORIZED_FOLDER_ID)) || folder.id == PeekTransitConstants.UNCATEGORIZED_FOLDER_ID

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .alpha(if (isEnabled) 1f else 0.5f)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (isEnabled) {
                                            if (checked && folder.id == PeekTransitConstants.UNCATEGORIZED_FOLDER_ID) {
                                                selectedFolderIds = setOf(PeekTransitConstants.UNCATEGORIZED_FOLDER_ID)
                                            }

                                            selectedFolderIds = if (checked) {
                                                selectedFolderIds + folder.id
                                            } else {
                                                selectedFolderIds - folder.id
                                            }
                                        }
                                    },
                                    enabled = isEnabled
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                FolderListItem(
                                    folder = folder,
                                    onClick = {
                                        if (isEnabled) {
                                            if (!isSelected && folder.id == PeekTransitConstants.UNCATEGORIZED_FOLDER_ID) {
                                                selectedFolderIds = setOf(PeekTransitConstants.UNCATEGORIZED_FOLDER_ID)
                                            }

                                            selectedFolderIds = if (isSelected) {
                                                selectedFolderIds - folder.id
                                            } else {
                                                selectedFolderIds + folder.id
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}