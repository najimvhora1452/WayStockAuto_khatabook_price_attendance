package com.example.ui.dialogs

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CartItemEntity
import com.example.data.InventoryItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model for a chunked slip page: Max 10 items per page with fixed clean size.
 */
data class OrderSlipPageData(
    val categoryName: String,
    val items: List<CartItemEntity>,
    val subPageIndex: Int,
    val totalSubPagesInCategory: Int,
    val globalPageIndex: Int,
    val totalGlobalPages: Int
)

@Composable
fun OrderSlipPreviewDialog(
    cartItems: List<CartItemEntity>,
    inventoryItems: List<InventoryItemEntity>,
    onDismiss: () -> Unit,
    onSlipSavedOrShared: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val currentDate = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()) }

    // Constant items limit per single page (Clean A4/Receipt sizing)
    val maxItemsPerPage = 10

    // Group cart items by category, then chunk into pages of max 10 items
    val slipPages = remember(cartItems) {
        val pagesList = mutableListOf<OrderSlipPageData>()
        val grouped = cartItems.groupBy { it.rootFolder }
        
        // Calculate total global pages across all categories
        var totalGlobal = 0
        grouped.forEach { (_, items) ->
            val chunks = items.chunked(maxItemsPerPage)
            totalGlobal += chunks.size
        }

        var globalIdx = 0
        grouped.forEach { (cat, items) ->
            val chunks = items.chunked(maxItemsPerPage)
            chunks.forEachIndexed { subIdx, chunkItems ->
                pagesList.add(
                    OrderSlipPageData(
                        categoryName = cat,
                        items = chunkItems,
                        subPageIndex = subIdx + 1,
                        totalSubPagesInCategory = chunks.size,
                        globalPageIndex = globalIdx + 1,
                        totalGlobalPages = totalGlobal.coerceAtLeast(1)
                    )
                )
                globalIdx++
            }
        }
        pagesList
    }

    // Selected page index for Full-Screen Centered Zoom View
    var zoomPageIndex by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("preview_section"),
        color = WayStockBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WayStockTextMain)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Order Slips Preview",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                text = "${slipPages.size} Slip Page${if (slipPages.size > 1) "s" else ""} • Max 10 items/page • Tap to enlarge",
                                fontSize = 11.sp,
                                color = WayStockTextSec
                            )
                        }
                    }
                }

                if (slipPages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items in Bucket to preview", color = WayStockDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    // 2-by-2 Grid Layout (1st top-left, 2nd top-right, 3rd bottom-left, 4th bottom-right...)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(slipPages) { index, pageData ->
                            SlipCardMiniature(
                                pageData = pageData,
                                inventoryItems = inventoryItems,
                                currentDate = currentDate,
                                onClick = { zoomPageIndex = index },
                                onSave = {
                                    val keys = pageData.items.map { it.key }
                                    val bitmap = com.example.util.OrderSlipBitmapGenerator.generateReceiptBitmap(
                                        rootFolder = if (pageData.totalSubPagesInCategory > 1) "${pageData.categoryName} (Part ${pageData.subPageIndex})" else pageData.categoryName,
                                        items = pageData.items,
                                        inventoryItems = inventoryItems,
                                        pageNumber = pageData.globalPageIndex,
                                        totalPages = pageData.totalGlobalPages,
                                        currentDate = currentDate
                                    )
                                    val success = com.example.util.OrderSlipBitmapGenerator.saveBitmapToGallery(
                                        context = context,
                                        bitmap = bitmap,
                                        category = "${pageData.categoryName}_P${pageData.subPageIndex}"
                                    )
                                    if (success) {
                                        // Only flush the downloaded page items, remaining pages stay safely in cart!
                                        onSlipSavedOrShared(keys)
                                    }
                                },
                                onShare = {
                                    val keys = pageData.items.map { it.key }
                                    val bitmap = com.example.util.OrderSlipBitmapGenerator.generateReceiptBitmap(
                                        rootFolder = if (pageData.totalSubPagesInCategory > 1) "${pageData.categoryName} (Part ${pageData.subPageIndex})" else pageData.categoryName,
                                        items = pageData.items,
                                        inventoryItems = inventoryItems,
                                        pageNumber = pageData.globalPageIndex,
                                        totalPages = pageData.totalGlobalPages,
                                        currentDate = currentDate
                                    )
                                    val success = com.example.util.OrderSlipBitmapGenerator.shareBitmap(
                                        context = context,
                                        bitmap = bitmap,
                                        category = "${pageData.categoryName}_P${pageData.subPageIndex}"
                                    )
                                    if (success) {
                                        onSlipSavedOrShared(keys)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Fullscreen Zoom Pager Modal (Centered, transparent blurred backdrop, sliding between pages)
            zoomPageIndex?.let { initialIndex ->
                FullScreenSlipPagerModal(
                    slipPages = slipPages,
                    inventoryItems = inventoryItems,
                    initialPageIndex = initialIndex,
                    currentDate = currentDate,
                    onDismiss = { zoomPageIndex = null },
                    onSaveOrShare = { keys ->
                        onSlipSavedOrShared(keys)
                    }
                )
            }
        }
    }
}

/**
 * 2x2 Grid Item Card (Compact slip preview with Top Action Options)
 */
@Composable
private fun SlipCardMiniature(
    pageData: OrderSlipPageData,
    inventoryItems: List<InventoryItemEntity>,
    currentDate: String,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(295.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .border(1.2.dp, WayStockBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("slip_card_${pageData.globalPageIndex}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Card Top Banner with Page Tag + Quick Action Buttons (Download, Share, Zoom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(WayStockPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    val pageLabel = if (pageData.totalSubPagesInCategory > 1) {
                        "P ${pageData.globalPageIndex}/${pageData.totalGlobalPages} (Pt ${pageData.subPageIndex})"
                    } else {
                        "P ${pageData.globalPageIndex}/${pageData.totalGlobalPages}"
                    }
                    Text(
                        text = pageLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockPrimaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Action Buttons on Top of Each Mini Page
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download/Save Button
                    Surface(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onSave() },
                        color = WayStockPrimary.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save Page ${pageData.globalPageIndex}",
                                tint = WayStockPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Share Button
                    Surface(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onShare() },
                        color = Color(0xFF16A34A).copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Page ${pageData.globalPageIndex}",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Zoom/Enlarge Button
                    Surface(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onClick() },
                        color = Color(0xFFF1F5F9)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Enlarge Page ${pageData.globalPageIndex}",
                                tint = WayStockDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Title
            Text(
                text = if (pageData.totalSubPagesInCategory > 1) "${pageData.categoryName.uppercase()} (${pageData.subPageIndex}/${pageData.totalSubPagesInCategory})" else pageData.categoryName.uppercase(),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WayStockDark,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${pageData.items.size} item(s) on this page",
                fontSize = 9.5.sp,
                color = WayStockTextSec,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )

            HorizontalDivider(
                color = WayStockPrimary.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 5.dp)
            )

            // Miniature item preview lines
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                pageData.items.take(4).forEachIndexed { idx, item ->
                    val parentKey = if (item.fullPath.contains(">")) {
                        item.fullPath.substringBeforeLast(">")
                    } else ""
                    val parentEntity = inventoryItems.find { it.key == parentKey }
                    val displayName = if (parentEntity?.toggleOn == true) {
                        "${parentEntity.name} ${item.name}"
                    } else item.name

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${idx + 1}. $displayName",
                            fontSize = 10.5.sp,
                            color = WayStockTextMain,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (pageData.items.size > 4) {
                    Text(
                        text = "+ ${pageData.items.size - 4} more on this slip...",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Tap to view full slip prompt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WayStockSelectedBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Tap to Enlarge 👆",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockPrimaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Fullscreen Slip Pager Modal (Centered on screen, elegant transparent blurred backdrop, clean sober receipt)
 */
@Composable
private fun FullScreenSlipPagerModal(
    slipPages: List<OrderSlipPageData>,
    inventoryItems: List<InventoryItemEntity>,
    initialPageIndex: Int,
    currentDate: String,
    onDismiss: () -> Unit,
    onSaveOrShare: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, (slipPages.size - 1).coerceAtLeast(0)),
        pageCount = { slipPages.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Transparent Dimmed & Blurred Scrim Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Centered Receipt Modal Content
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .widthIn(max = 480.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercepts clicks on receipt from dismissing dialog
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Nav & Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Page Indicator pill
                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        val curr = if (pagerState.currentPage in slipPages.indices) slipPages[pagerState.currentPage] else null
                        val titleText = if (curr != null && curr.totalSubPagesInCategory > 1) {
                            "Slip ${curr.globalPageIndex} of ${curr.totalGlobalPages} (${curr.categoryName} Part ${curr.subPageIndex})"
                        } else {
                            "Slip ${pagerState.currentPage + 1} of ${slipPages.size}"
                        }
                        Text(
                            text = titleText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Navigation Chevrons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = if (pagerState.currentPage > 0) 0.25f else 0.1f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < slipPages.size - 1) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            },
                            enabled = pagerState.currentPage < slipPages.size - 1,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = if (pagerState.currentPage < slipPages.size - 1) 0.25f else 0.1f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Horizontal Pager for Centered Receipt Card
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    if (page in slipPages.indices) {
                        val pageData = slipPages[page]

                        FullSlipReceiptCard(
                            pageData = pageData,
                            inventoryItems = inventoryItems,
                            currentDate = currentDate,
                            onSave = {
                                val keys = pageData.items.map { it.key }
                                val bitmap = com.example.util.OrderSlipBitmapGenerator.generateReceiptBitmap(
                                    rootFolder = if (pageData.totalSubPagesInCategory > 1) "${pageData.categoryName} (Part ${pageData.subPageIndex})" else pageData.categoryName,
                                    items = pageData.items,
                                    inventoryItems = inventoryItems,
                                    pageNumber = pageData.globalPageIndex,
                                    totalPages = pageData.totalGlobalPages,
                                    currentDate = currentDate
                                )
                                val success = com.example.util.OrderSlipBitmapGenerator.saveBitmapToGallery(
                                    context = context,
                                    bitmap = bitmap,
                                    category = "${pageData.categoryName}_P${pageData.subPageIndex}"
                                )
                                if (success) {
                                    onSaveOrShare(keys)
                                }
                            },
                            onShare = {
                                val keys = pageData.items.map { it.key }
                                val bitmap = com.example.util.OrderSlipBitmapGenerator.generateReceiptBitmap(
                                    rootFolder = if (pageData.totalSubPagesInCategory > 1) "${pageData.categoryName} (Part ${pageData.subPageIndex})" else pageData.categoryName,
                                    items = pageData.items,
                                    inventoryItems = inventoryItems,
                                    pageNumber = pageData.globalPageIndex,
                                    totalPages = pageData.totalGlobalPages,
                                    currentDate = currentDate
                                )
                                val success = com.example.util.OrderSlipBitmapGenerator.shareBitmap(
                                    context = context,
                                    bitmap = bitmap,
                                    category = "${pageData.categoryName}_P${pageData.subPageIndex}"
                                )
                                if (success) {
                                    onSaveOrShare(keys)
                                }
                            }
                        )
                    }
                }

                if (slipPages.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "👈 Swipe left/right to browse slips 👉",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Sober, clean, professional receipt card shown inside full screen pager
 */
@Composable
private fun FullSlipReceiptCard(
    pageData: OrderSlipPageData,
    inventoryItems: List<InventoryItemEntity>,
    currentDate: String,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .border(2.dp, WayStockPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            // Receipt Header Brand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📦 WAYSTOCK ORDER SLIP",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    val catTitle = if (pageData.totalSubPagesInCategory > 1) {
                        "${pageData.categoryName.uppercase()} (PART ${pageData.subPageIndex})"
                    } else {
                        pageData.categoryName.uppercase()
                    }
                    Text(
                        text = catTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WayStockDark,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = WayStockPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "P ${pageData.globalPageIndex}/${pageData.totalGlobalPages}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "Items: ${pageData.items.size} | $currentDate",
                fontSize = 11.sp,
                color = WayStockTextSec,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Table Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SR. ITEM DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontFamily = FontFamily.Monospace)
                    Text("QTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable Item List (Fixed clear layout for max 10 items)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                pageData.items.forEachIndexed { idx, item ->
                    val parentKey = if (item.fullPath.contains(">")) {
                        item.fullPath.substringBeforeLast(">")
                    } else ""

                    val parentEntity = inventoryItems.find { it.key == parentKey }
                    val displayName = if (parentEntity?.toggleOn == true) {
                        "${parentEntity.name} ${item.name}"
                    } else item.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. $displayName",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WayStockTextMain,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${item.quantity} ${item.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = WayStockBorder, thickness = 0.8.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = WayStockDark, thickness = 1.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer with Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WayStock Official Slip",
                    fontSize = 10.5.sp,
                    color = WayStockTextSec,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("download_slip_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Slip", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("share_slip_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
