package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InventoryItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BulkAddModal(
    currentParentFolder: String,
    editTargetKey: String?,
    allExistingItems: List<InventoryItemEntity>,
    initialText: String = "",
    onDismiss: () -> Unit,
    onSubmitStructure: (String) -> Unit
) {
    var rawInputText by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val displayFolderName = if (currentParentFolder == "root") "Home" else {
        if (currentParentFolder.contains(">")) currentParentFolder.split(">").last().trim() else currentParentFolder
    }

    val isEditMode = editTargetKey != null

    val handleDismiss: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    val handleSubmit: () -> Unit = {
        if (rawInputText.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onSubmitStructure(rawInputText)
        }
    }

    // Autocomplete hint chips matching current typing word
    val hintMatches = remember(rawInputText, allExistingItems) {
        if (rawInputText.isBlank() || isEditMode) emptyList()
        else {
            val lastLine = rawInputText.split("\n").lastOrNull() ?: ""
            val lastSegment = lastLine.split(">", ",").lastOrNull()?.trim()?.lowercase() ?: ""
            if (lastSegment.length >= 2) {
                allExistingItems.map { it.name }.distinct().filter {
                    it.lowercase().startsWith(lastSegment) && it.lowercase() != lastSegment
                }.take(4)
            } else emptyList()
        }
    }

    Dialog(onDismissRequest = handleDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("action_modal"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isEditMode) "Edit Name" else "Add inside: $displayFolderName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockTextSec
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Hint Chips Row
                if (hintMatches.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        items(hintMatches) { hint ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val lines = rawInputText.split("\n").toMutableList()
                                        val lastLine = lines.lastOrNull() ?: ""
                                        val parts = lastLine.split(">").toMutableList()
                                        parts[parts.size - 1] = " $hint"
                                        lines[lines.size - 1] = parts.joinToString(" > ")
                                        rawInputText = lines.joinToString("\n")
                                    },
                                color = Color(0xFFE6F4F5),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "💡 $hint",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockPrimary
                                )
                            }
                        }
                    }
                }

                // Textarea Field
                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    placeholder = {
                        Text(
                            text = if (isEditMode) "Enter new name..." else "Type item or paste structure e.g:\nLays, Bingo, Pringles\nor Category > Subcategory > Item",
                            fontSize = 13.sp,
                            color = WayStockTextSec
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .focusRequester(focusRequester)
                        .testTag("modal_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WayStockDark,
                        unfocusedTextColor = WayStockDark,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = handleDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Text("Cancel", color = WayStockTextSec, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = handleSubmit,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("modal_action_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Text(
                            text = if (isEditMode) "Save Changes ✏️" else "Create Structure 🚀",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
