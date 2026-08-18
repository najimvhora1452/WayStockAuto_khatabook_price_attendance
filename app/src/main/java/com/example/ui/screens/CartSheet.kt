package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItemEntity
import com.example.data.InventoryItemEntity
import com.example.ui.dialogs.AddCustomItemDialog
import com.example.ui.theme.*

@Composable
fun CartSheet(
    cartItems: List<CartItemEntity>,
    allInventoryItems: List<InventoryItemEntity>,
    isAdminMode: Boolean,
    onDismiss: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onSetQuantityDirectly: (String, Int) -> Unit,
    onUpdateUnit: (String, String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onGeneratePreview: () -> Unit,
    onAddCustomUnit: (String, String) -> Unit,
    onDeleteCustomUnit: (String, String) -> Unit,
    onAddCustomItem: (name: String, category: String, quantity: Int, unit: String) -> Unit = { _, _, _, _ -> }
) {
    var isAddCustomItemDialogOpen by remember { mutableStateOf(false) }
    val availableRootCategories = remember(allInventoryItems) {
        val rootFolders = allInventoryItems.filter { it.parent == "root" && it.type == "folder" }.map { it.name }
        if (rootFolders.isNotEmpty()) rootFolders.distinct()
        else {
            val allRoots = allInventoryItems.filter { it.parent == "root" }.map { it.name }
            if (allRoots.isNotEmpty()) allRoots.distinct() else listOf("Snacks", "Beverages", "Paan Masala", "Cigarettes", "Perfumes")
        }
    }

    if (isAddCustomItemDialogOpen) {
        AddCustomItemDialog(
            availableCategories = availableRootCategories,
            onDismiss = { isAddCustomItemDialogOpen = false },
            onAddCustomItem = { name, category, qty, unit ->
                onAddCustomItem(name, category, qty, unit)
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_section"),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_cart_btn")) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close", tint = WayStockTextMain)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Bucket", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    }

                    // "+ Custom Item" Top Bar Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { isAddCustomItemDialogOpen = true }
                            .testTag("add_custom_item_topbar_btn"),
                        color = WayStockPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Item",
                                tint = WayStockPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Custom Item",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockPrimary
                            )
                        }
                    }
                }
            }

            if (cartItems.isEmpty()) {
                // Empty Bucket State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Empty",
                        tint = WayStockPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Bucket Khali Hai!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    Text(
                        "Lagta hai aapne abhi tak kuch select nahi kiya. Chalo kuch naya dhundhte hain!",
                        fontSize = 13.sp,
                        color = WayStockTextSec,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                    )
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Text("Maal Bharo! 🚀", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Cart Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(cartItems, key = { it.key }) { item ->
                        val invEntity = allInventoryItems.find { it.key == item.key }
                        val allowedUnits = remember(invEntity?.allowedUnitsCsv) {
                            invEntity?.allowedUnitsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf("Box", "Packet", "Bunch", "Kg")
                        }

                        var unitMenuExpanded by remember { mutableStateOf(false) }
                        var newUnitInput by remember { mutableStateOf("") }

                        val parentKey = if (item.fullPath.contains(">")) item.fullPath.substringBeforeLast(">") else ""
                        val parentEntity = allInventoryItems.find { it.key == parentKey }
                        val displayName = if (parentEntity?.toggleOn == true) "${parentEntity.name} ${item.name}" else item.name

                        val isCustomItem = item.key.startsWith("custom_")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Item Title + Custom Indicator
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WayStockTextMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCustomItem) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Custom",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                                if (isCustomItem && item.rootFolder.isNotBlank()) {
                                    Text(
                                        text = "📁 ${item.rootFolder}",
                                        fontSize = 11.sp,
                                        color = WayStockTextSec,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Actions: Quantity Capsule + Unit Chip + Delete Icon (Fixed Uniform Widths for Perfect Column Alignment)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Gesture Capsule Quantity Controller (Fixed 88.dp)
                                var dragAccumulator by remember { mutableFloatStateOf(0f) }

                                Box(
                                    modifier = Modifier
                                        .width(88.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .border(1.dp, WayStockBorder, RoundedCornerShape(18.dp))
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = { dragAccumulator = 0f },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    dragAccumulator += dragAmount
                                                    if (dragAccumulator > 30f) {
                                                        onUpdateQuantity(item.key, 1)
                                                        dragAccumulator = 0f
                                                    } else if (dragAccumulator < -30f) {
                                                        onUpdateQuantity(item.key, -1)
                                                        dragAccumulator = 0f
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.key, -1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = WayStockTextMain, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            text = item.quantity.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WayStockDark
                                        )

                                        IconButton(
                                            onClick = { onUpdateQuantity(item.key, 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = WayStockTextMain, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                // Unit Chip (Fixed 76.dp Width for Strict Grid Alignment)
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { unitMenuExpanded = true }
                                            .border(1.dp, WayStockBorder, RoundedCornerShape(8.dp)),
                                        color = Color(0xFFF8FAFC)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.unit,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WayStockDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = unitMenuExpanded,
                                        onDismissRequest = { unitMenuExpanded = false },
                                        shadowElevation = 12.dp,
                                        modifier = Modifier
                                            .width(160.dp)
                                            .background(Color.White)
                                            .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        allowedUnits.forEach { unit ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(unit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                                        if (isAdminMode) {
                                                            IconButton(
                                                                onClick = { onDeleteCustomUnit(item.key, unit) },
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = WayStockDanger, modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    unitMenuExpanded = false
                                                    onUpdateUnit(item.key, unit)
                                                }
                                            )
                                        }

                                        if (isAdminMode) {
                                            HorizontalDivider(color = WayStockBorder)
                                            Box(modifier = Modifier.padding(8.dp)) {
                                                OutlinedTextField(
                                                    value = newUnitInput,
                                                    onValueChange = { newUnitInput = it },
                                                    placeholder = { Text("+ Unit", fontSize = 11.sp, color = WayStockTextSec) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = WayStockDark,
                                                        unfocusedTextColor = WayStockDark,
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White,
                                                        focusedBorderColor = WayStockPrimary,
                                                        unfocusedBorderColor = WayStockBorder
                                                    ),
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = {
                                                                if (newUnitInput.isNotBlank()) {
                                                                    onAddCustomUnit(item.key, newUnitInput)
                                                                    newUnitInput = ""
                                                                    unitMenuExpanded = false
                                                                }
                                                            }
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = "Add", tint = WayStockPrimary)
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }

                                // Delete item (Fixed 32.dp Width)
                                IconButton(
                                    onClick = { onRemoveItem(item.key) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Remove",
                                        tint = WayStockTextSec,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }

                // Footer Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Items:", fontSize = 12.sp, color = WayStockTextSec)
                            Text(
                                text = cartItems.size.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WayStockPrimary
                            )
                        }

                        Button(
                            onClick = onGeneratePreview,
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("preview_order_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                        ) {
                            Text("Preview Order 🚀", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
