package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItemEntity
import com.example.data.InventoryItemEntity
import com.example.data.SearchHistoryEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchOverlay(
    allInventoryItems: List<InventoryItemEntity>,
    cartItems: List<CartItemEntity>,
    searchHistory: List<SearchHistoryEntity>,
    onDismiss: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    onVoiceCommand: (String) -> Unit,
    onTriggerAdmin: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isListeningVoice by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus and open keyboard on launch
    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    fun checkAndTriggerAdmin(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.equals("admin.html", ignoreCase = true) ||
            trimmed.equals("admin", ignoreCase = true) && trimmed.length >= 5 /* exact check */) {
            if (trimmed.equals("admin.html", ignoreCase = true)) {
                focusManager.clearFocus()
                keyboardController?.hide()
                onDismiss()
                if (onTriggerAdmin != null) {
                    onTriggerAdmin()
                } else {
                    onVoiceCommand("admin.html")
                }
                return true
            }
        }
        return false
    }

    val filteredMatches = remember(searchQuery, allInventoryItems) {
        if (searchQuery.isBlank() || searchQuery.trim().equals("admin.html", ignoreCase = true)) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allInventoryItems.filter {
                it.name.lowercase().contains(q) || it.key.lowercase().contains(q)
            }.take(10)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_overlay"),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WayStockTextMain
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = WayStockPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search items...",
                                        fontSize = 14.5.sp,
                                        color = WayStockTextSec,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        if (it.trim().equals("admin.html", ignoreCase = true)) {
                                            checkAndTriggerAdmin(it)
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = WayStockDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(WayStockPrimary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (!checkAndTriggerAdmin(searchQuery)) {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .testTag("main_search_input")
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = WayStockTextSec,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Mic Button for Voice Commands
                            IconButton(
                                onClick = {
                                    isListeningVoice = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("voice_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results / Suggestions Area
            if (searchQuery.isNotBlank()) {
                if (filteredMatches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for '$searchQuery'",
                            fontSize = 14.sp,
                            color = WayStockTextSec
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredMatches, key = { it.key }) { item ->
                            val isInCart = cartItems.any { it.key == item.key }
                            val folderPath = if (item.key.contains(">")) {
                                val parts = item.key.split(">")
                                parts.subList(0, parts.size - 1).joinToString(" > ")
                            } else {
                                if (item.parent != "root" && item.parent != "Home") item.parent else "Main Category"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        if (item.name.lowercase() == "admin.html") {
                                            onVoiceCommand("admin.html")
                                        } else {
                                            onSuggestionClick(item.key)
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (item.type == "folder") {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Inventory2,
                                            contentDescription = null,
                                            tint = WayStockTealMedium,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Folder / Category Name in small text above
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = folderPath,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // Item or Folder Name
                                        Text(
                                            text = item.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WayStockTextMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (item.type == "item") {
                                    if (isInCart) {
                                        Surface(
                                            color = Color(0xFFECFDF5),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "✓ Added",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = WayStockSuccess
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { onAddToCart(item.key) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ShoppingCart,
                                                contentDescription = "Add",
                                                tint = WayStockTextSec,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            } else {
                // Recent Searches
                if (searchHistory.isNotEmpty()) {
                    Text(
                        text = "⏱️ RECENT SEARCHES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockTextSec,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchHistory, key = { it.id }) { history ->
                            val matchedItem = allInventoryItems.find { it.key == history.itemKey }
                            val folderPath = if (history.itemKey.contains(">")) {
                                val segments = history.itemKey.split(">")
                                segments.subList(0, segments.size - 1).joinToString(" > ")
                            } else {
                                matchedItem?.parent?.takeIf { it != "root" && it != "Home" } ?: "Main Category"
                            }
                            val itemName = history.queryText.ifBlank {
                                matchedItem?.name ?: history.itemKey.substringAfterLast(">")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onSuggestionClick(history.itemKey)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (matchedItem?.type == "folder") Icons.Outlined.Folder else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (matchedItem?.type == "folder") Color(0xFFF59E0B) else WayStockTextSec,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    // Folder / Category Name in small text above item name
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = folderPath,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Main Item Name
                                    Text(
                                        text = itemName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WayStockTextMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // Rich Voice Assistant Dialog with Speech Recognition & Quick Actions
        if (isListeningVoice) {
            VoiceAssistantDialog(
                onDismiss = { isListeningVoice = false },
                onExecuteCommand = { cmd ->
                    isListeningVoice = false
                    onVoiceCommand(cmd)
                }
            )
        }
    }
}
