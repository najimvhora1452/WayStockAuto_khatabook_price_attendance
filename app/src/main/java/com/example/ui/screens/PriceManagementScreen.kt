package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItemEntity
import com.example.ui.WayStockViewModel
import com.example.ui.dialogs.PriceHistoryDialog
import com.example.ui.dialogs.PriceUpdateDialog
import com.example.ui.dialogs.ShareRateCardDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceManagementScreen(viewModel: WayStockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val allInventoryItems by viewModel.allInventoryItems.collectAsState()
    val allPriceHistory by viewModel.allPriceHistory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HIKED, DROPPED, HIGH_MARGIN, LOW_MARGIN, CATEGORY
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var itemToEditPrice by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var itemForHistoryView by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var isAllHistoryOpen by remember { mutableStateOf(false) }
    var isShareRateCardOpen by remember { mutableStateOf(false) }

    // Filter only concrete items (not folder structures)
    val actualItems = remember(allInventoryItems) {
        allInventoryItems.filter { it.type == "item" }
    }

    // Extract categories
    val categories = remember(actualItems) {
        actualItems.map { it.parent.substringBefore(">") }.distinct().filter { it.isNotBlank() }
    }

    // Filtered items based on search and filters
    val filteredItems = remember(actualItems, searchQuery, selectedFilter, selectedCategoryFilter) {
        actualItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.displayName.contains(searchQuery, ignoreCase = true) ||
                    item.parent.contains(searchQuery, ignoreCase = true)

            val mrpDiff = item.mrp - item.previousMrp
            val wholesaleMargin = if (item.costPrice > 0) ((item.wholesalePrice - item.costPrice) / item.costPrice) * 100 else 0.0

            val matchesFilter = when (selectedFilter) {
                "HIKED" -> mrpDiff > 0 || (item.wholesalePrice > item.previousWholesale && item.previousWholesale > 0)
                "DROPPED" -> mrpDiff < 0 || (item.wholesalePrice < item.previousWholesale && item.previousWholesale > 0)
                "HIGH_MARGIN" -> wholesaleMargin >= 20.0
                "LOW_MARGIN" -> wholesaleMargin in 0.1..10.0
                "CATEGORY" -> selectedCategoryFilter == null || item.parent.startsWith(selectedCategoryFilter!!)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    // Analytics summary
    val totalItemsCount = actualItems.size
    val avgRetailMargin = remember(actualItems) {
        val valid = actualItems.filter { it.costPrice > 0 && it.mrp > 0 }
        if (valid.isNotEmpty()) {
            valid.map { ((it.mrp - it.costPrice) / it.costPrice) * 100 }.average()
        } else 0.0
    }
    val avgWholesaleMargin = remember(actualItems) {
        val valid = actualItems.filter { it.costPrice > 0 && it.wholesalePrice > 0 }
        if (valid.isNotEmpty()) {
            valid.map { ((it.wholesalePrice - it.costPrice) / it.costPrice) * 100 }.average()
        } else 0.0
    }
    val recentPriceHikesCount = remember(actualItems) {
        actualItems.count { it.mrp > it.previousMrp && it.previousMrp > 0 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("price_management_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar / Header
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFEDD5),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Sell,
                                        contentDescription = "Price Tracker",
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Price & Margin Tracker",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockDark
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.isSuperAdmin) {
                                        Text(
                                            "👑 Super Premium (Owner)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    } else {
                                        Text(
                                            "⭐ Premium Authorized Staff",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Price History Button
                            IconButton(
                                onClick = { isAllHistoryOpen = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "All History",
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Share Rate Card Button
                            IconButton(
                                onClick = { isShareRateCardOpen = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share Rate Card",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metric Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total items
                        MetricTile(
                            title = "Tracked Items",
                            value = "$totalItemsCount",
                            subText = "Live Prices",
                            color = WayStockPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Wholesale Margin
                        MetricTile(
                            title = "Avg Wholesale",
                            value = "${String.format(Locale.US, "%.1f", avgWholesaleMargin)}%",
                            subText = "B2B Margin",
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )

                        // Retail Margin
                        MetricTile(
                            title = "Avg Retail MRP",
                            value = "${String.format(Locale.US, "%.1f", avgRetailMargin)}%",
                            subText = "Profit %",
                            color = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )

                        // Recent Hikes
                        MetricTile(
                            title = "Price Hikes",
                            value = "$recentPriceHikesCount",
                            subText = "📈 Trend",
                            color = Color(0xFFEA580C),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search item name, brand or category...", fontSize = 13.sp, color = WayStockTextSec) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = WayStockTextSec, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = WayStockTextSec, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WayStockDark,
                            unfocusedTextColor = WayStockDark,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("price_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChipItem(
                                title = "All (${actualItems.size})",
                                isSelected = selectedFilter == "ALL",
                                onClick = {
                                    selectedFilter = "ALL"
                                    selectedCategoryFilter = null
                                }
                            )
                        }
                        item {
                            FilterChipItem(
                                title = "📈 Price Hikes ($recentPriceHikesCount)",
                                isSelected = selectedFilter == "HIKED",
                                onClick = { selectedFilter = "HIKED" }
                            )
                        }
                        item {
                            FilterChipItem(
                                title = "💎 High Margin (>20%)",
                                isSelected = selectedFilter == "HIGH_MARGIN",
                                onClick = { selectedFilter = "HIGH_MARGIN" }
                            )
                        }
                        item {
                            FilterChipItem(
                                title = "⚠️ Low Margin (<10%)",
                                isSelected = selectedFilter == "LOW_MARGIN",
                                onClick = { selectedFilter = "LOW_MARGIN" }
                            )
                        }
                        items(categories) { cat ->
                            FilterChipItem(
                                title = "📁 $cat",
                                isSelected = selectedFilter == "CATEGORY" && selectedCategoryFilter == cat,
                                onClick = {
                                    selectedFilter = "CATEGORY"
                                    selectedCategoryFilter = cat
                                }
                            )
                        }
                    }
                }
            }

            // 2. Price Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No items match the selected filter.", fontWeight = FontWeight.Bold, color = WayStockDark)
                        Text("Search query badal kar dekhein ya All Items select karein.", fontSize = 12.sp, color = WayStockTextSec)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.key }) { item ->
                        ItemPriceCard(
                            item = item,
                            onEditPrice = { itemToEditPrice = item },
                            onViewHistory = { itemForHistoryView = item }
                        )
                    }
                }
            }
        }

        // Price Update Modal
        itemToEditPrice?.let { item ->
            PriceUpdateDialog(
                item = item,
                onDismiss = { itemToEditPrice = null },
                onSavePrice = { newMrp, newWholesale, newCost, note ->
                    val updaterName = if (uiState.isSuperAdmin) "Najim Vhora (Super Admin)" else (uiState.loggedInAdminName ?: "Admin")
                    viewModel.updateItemPrice(
                        item = item,
                        newMrp = newMrp,
                        newWholesale = newWholesale,
                        newCost = newCost,
                        note = note,
                        updatedBy = updaterName
                    )
                    itemToEditPrice = null
                }
            )
        }

        // Single Item Price History Modal
        itemForHistoryView?.let { item ->
            PriceHistoryDialog(
                historyList = allPriceHistory,
                filterItemName = item.displayName.ifBlank { item.name },
                isSuperAdmin = uiState.isSuperAdmin,
                onDismiss = { itemForHistoryView = null },
                onDeleteHistoryItem = { id -> viewModel.deletePriceHistory(id) }
            )
        }

        // All Items Price History Modal
        if (isAllHistoryOpen) {
            PriceHistoryDialog(
                historyList = allPriceHistory,
                filterItemName = null,
                isSuperAdmin = uiState.isSuperAdmin,
                onDismiss = { isAllHistoryOpen = false },
                onDeleteHistoryItem = { id -> viewModel.deletePriceHistory(id) }
            )
        }

        // Share Rate Card Modal
        if (isShareRateCardOpen) {
            ShareRateCardDialog(
                items = allInventoryItems,
                onDismiss = { isShareRateCardOpen = false }
            )
        }
    }
}

@Composable
fun ItemPriceCard(
    item: InventoryItemEntity,
    onEditPrice: () -> Unit,
    onViewHistory: () -> Unit
) {
    val mrpProfit = item.mrp - item.costPrice
    val mrpMargin = if (item.costPrice > 0) (mrpProfit / item.costPrice) * 100 else 0.0

    val wholesaleProfit = item.wholesalePrice - item.costPrice
    val wholesaleMargin = if (item.costPrice > 0) (wholesaleProfit / item.costPrice) * 100 else 0.0

    val mrpDiff = item.mrp - item.previousMrp
    val wholesaleDiff = item.wholesalePrice - item.previousWholesale

    val lastUpdatedDate = remember(item.lastPriceUpdated) {
        if (item.lastPriceUpdated > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(item.lastPriceUpdated))
        } else {
            "Standard"
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Item Name, Category, Allowed Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName.ifBlank { item.name },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                item.parent,
                                fontSize = 10.5.sp,
                                color = WayStockTextSec,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Unit: ${item.currentUnit}",
                            fontSize = 11.sp,
                            color = WayStockPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Price Trend Badge if price was altered
                if (mrpDiff != 0.0 || wholesaleDiff != 0.0) {
                    val isHike = mrpDiff > 0 || wholesaleDiff > 0
                    Surface(
                        color = if (isHike) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHike) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (isHike) "+₹${if (mrpDiff > 0) mrpDiff else wholesaleDiff} 📈" else "-₹${if (mrpDiff < 0) -mrpDiff else -wholesaleDiff} 📉",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHike) WayStockDanger else Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Pillar Price Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Buying Cost
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Purchase Cost", fontSize = 10.5.sp, color = WayStockTextSec)
                    Text(
                        if (item.costPrice > 0) "₹${item.costPrice}" else "₹0.00",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark
                    )
                    Text("Base Cost", fontSize = 10.sp, color = WayStockTextSec)
                }

                // Wholesale Rate (B2B)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wholesale Rate", fontSize = 10.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (item.wholesalePrice > 0) "₹${item.wholesalePrice}" else "-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        if (item.costPrice > 0) "+${String.format(Locale.US, "%.1f", wholesaleMargin)}% margin" else "B2B",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                // Retail MRP (B2C)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Retail MRP", fontSize = 10.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (item.mrp > 0) "₹${item.mrp}" else "-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        if (item.costPrice > 0) "+${String.format(Locale.US, "%.1f", mrpMargin)}% margin" else "MRP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Last updated & Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Updated: $lastUpdatedDate",
                    fontSize = 10.5.sp,
                    color = WayStockTextSec
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // History Icon Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.clickable { onViewHistory() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }

                    // Edit Price Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEDD5),
                        modifier = Modifier.clickable { onEditPrice() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = Color(0xFFEA580C), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.5.sp, color = WayStockTextSec, maxLines = 1)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(subText, fontSize = 9.sp, color = color)
        }
    }
}

@Composable
fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) WayStockPrimary else Color(0xFFF1F5F9),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else WayStockDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
