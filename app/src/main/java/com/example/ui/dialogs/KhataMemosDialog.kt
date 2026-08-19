package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.KhataMemoEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KhataMemosDialog(
    memos: List<KhataMemoEntity>,
    isStickyBottomMemoBarEnabled: Boolean = true,
    onToggleStickyBottomMemoBar: (Boolean) -> Unit = {},
    isStickyNotificationEnabled: Boolean = false,
    onToggleStickyNotification: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onDeleteMemo: (Long) -> Unit,
    onClearAll: () -> Unit,
    onAddMemo: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    var newMemoText by remember { mutableStateOf("") }
    var copiedMemoId by remember { mutableStateOf<Long?>(null) }
    var showTogglesCard by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("khata_memos_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WayStockPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Khata Notifications & Memos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                text = "${memos.size} active reminders",
                                fontSize = 11.5.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showTogglesCard = !showTogglesCard },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Preferences",
                                tint = if (showTogglesCard) WayStockPrimary else WayStockTextSec,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                        }
                    }
                }

                // Expandable Toggles Card (Sticky Bar & Status Bar Notification)
                if (showTogglesCard) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚙️ Sticky Bar & Notification Preferences",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // 1. Bottom Memo Bar Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📌 Bottom Quick Memo Bar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                    Text("Show sticky input bar at bottom of customer list", fontSize = 10.5.sp, color = WayStockTextSec)
                                }
                                Switch(
                                    checked = isStickyBottomMemoBarEnabled,
                                    onCheckedChange = onToggleStickyBottomMemoBar,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = WayStockPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Persistent System Notification Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🔔 Phone Status Bar Notification", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                    Text("Persistent notification bar for 1-tap khata entry", fontSize = 10.5.sp, color = WayStockTextSec)
                                }
                                Switch(
                                    checked = isStickyNotificationEnabled,
                                    onCheckedChange = onToggleStickyNotification,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field for Adding Memo directly inside Dialog
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = newMemoText,
                            onValueChange = { newMemoText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = WayStockDark,
                                fontWeight = FontWeight.Medium
                            ),
                            decorationBox = { innerTextField ->
                                if (newMemoText.isEmpty()) {
                                    Text(
                                        "Write a new memo or reminder...",
                                        fontSize = 12.5.sp,
                                        color = WayStockTextSec
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newMemoText.isNotBlank()) {
                                    onAddMemo(newMemoText)
                                    newMemoText = ""
                                }
                            },
                            enabled = newMemoText.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Clear All if not empty
                if (memos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pinned Notes & Ledger Memos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WayStockTextSec
                        )
                        TextButton(
                            onClick = onClearAll,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = WayStockDanger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 11.sp, color = WayStockDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // List of Memos
                if (memos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = WayStockTextSec.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Active Notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                text = "Use the sticky bar at the bottom of Khata to add notes, payment promises, or reminders.",
                                fontSize = 11.5.sp,
                                color = WayStockTextSec,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(memos, key = { it.id }) { memo ->
                            val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(memo.timestamp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = WayStockPrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("📌", fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memo.note,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WayStockDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 10.5.sp,
                                            color = WayStockTextSec
                                        )
                                    }

                                    // Copy Action
                                    IconButton(
                                        onClick = {
                                            clipboardManager?.setPrimaryClip(
                                                ClipData.newPlainText("Khata Note", memo.note)
                                            )
                                            copiedMemoId = memo.id
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            if (copiedMemoId == memo.id) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = if (copiedMemoId == memo.id) Color(0xFF16A34A) else WayStockTextSec,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete Action
                                    IconButton(
                                        onClick = { onDeleteMemo(memo.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = WayStockDanger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockDark)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
