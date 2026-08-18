package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PriceHistoryEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceHistoryDialog(
    historyList: List<PriceHistoryEntity>,
    filterItemName: String? = null,
    isSuperAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onDeleteHistoryItem: (Long) -> Unit = {}
) {
    val filteredHistory = remember(historyList, filterItemName) {
        if (filterItemName != null) {
            historyList.filter { it.itemName.equals(filterItemName, ignoreCase = true) || it.itemKey == filterItemName }
        } else {
            historyList
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("price_history_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Price History",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (filterItemName != null) "Price History Log" else "All Price Changes Timeline",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                if (filterItemName != null) filterItemName else "${filteredHistory.size} recorded revisions",
                                fontSize = 12.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🕒", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No price changes recorded yet.", fontWeight = FontWeight.Bold, color = WayStockDark)
                            Text("Price change history will appear here once items are updated.", fontSize = 12.sp, color = WayStockTextSec)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredHistory, key = { it.id }) { record ->
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dateStr = sdf.format(Date(record.timestamp))

                            val mrpDiff = record.newMrp - record.oldMrp
                            val wholesaleDiff = record.newWholesale - record.oldWholesale

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                record.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = WayStockDark
                                            )
                                            Text(
                                                dateStr,
                                                fontSize = 11.sp,
                                                color = WayStockTextSec
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "By ${record.updatedBy}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }

                                            if (isSuperAdmin) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { onDeleteHistoryItem(record.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Delete record",
                                                        tint = WayStockDanger,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Rate Comparison
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Wholesale Price Change
                                        Column {
                                            Text("Wholesale Rate", fontSize = 10.5.sp, color = WayStockTextSec)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("₹${record.oldWholesale}", fontSize = 12.sp, color = WayStockTextSec)
                                                Text(" ➔ ", fontSize = 11.sp, color = WayStockTextSec)
                                                Text("₹${record.newWholesale}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                                if (wholesaleDiff != 0.0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (wholesaleDiff > 0) "+₹$wholesaleDiff 📈" else "-₹${-wholesaleDiff} 📉",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (wholesaleDiff > 0) Color(0xFF16A34A) else WayStockDanger
                                                    )
                                                }
                                            }
                                        }

                                        // Retail MRP Change
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Retail MRP", fontSize = 10.5.sp, color = WayStockTextSec)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("₹${record.oldMrp}", fontSize = 12.sp, color = WayStockTextSec)
                                                Text(" ➔ ", fontSize = 11.sp, color = WayStockTextSec)
                                                Text("₹${record.newMrp}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                                if (mrpDiff != 0.0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (mrpDiff > 0) "+₹$mrpDiff 📈" else "-₹${-mrpDiff} 📉",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (mrpDiff > 0) Color(0xFF16A34A) else WayStockDanger
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (record.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFFFFFBEB),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "📝 Reason: ${record.note}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Text("Close History", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
