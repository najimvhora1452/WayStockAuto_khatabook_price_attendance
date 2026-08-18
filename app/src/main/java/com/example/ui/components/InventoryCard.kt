package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItemEntity
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryCard(
    item: InventoryItemEntity,
    cartQuantity: Int,
    cartUnit: String,
    isAdminMode: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onAddToCart: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    onFolderToggleChange: (Boolean) -> Unit,
    onAddSubItemClick: () -> Unit,
    onUnitSelected: (String) -> Unit,
    onAddCustomUnit: (String) -> Unit,
    onDeleteCustomUnit: (String) -> Unit
) {
    val isFolder = item.type == "folder"
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var newUnitInputText by remember { mutableStateOf("") }

    // 3D Flip animation state
    val flipRotation by animateFloatAsState(
        targetValue = if (isSelected) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    val cardBg = if (isSelected) WayStockSelectedBg else Color.White
    val cardBorder = if (isSelected) WayStockSelectedBorder else WayStockBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onCardLongClick()
                    } else {
                        onCardClick()
                    }
                },
                onLongClick = onCardLongClick
            )
            .testTag("inventory_card_${item.key}"),
        color = cardBg,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Section: Admin plus button + Icon + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAdminMode) {
                        IconButton(
                            onClick = onAddSubItemClick,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .testTag("add_sub_item_${item.key}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isFolder) "Add inside folder" else "Add sub-items (convert to folder)",
                                tint = WayStockPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Flip Icon Box
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                rotationY = flipRotation
                                cameraDistance = 12f * density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipRotation > 90f) {
                            // Checkmark when selected
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WayStockCyan, CircleShape)
                                    .graphicsLayer { rotationY = 180f },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            if (isFolder) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = "Folder",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = "Item",
                                    tint = WayStockTealMedium,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
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

                // Right Section: Toggle (Admin Folder) or Cart Controls & Unit selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdminMode && isFolder) {
                        Switch(
                            checked = item.toggleOn,
                            onCheckedChange = onFolderToggleChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WayStockSuccess
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("folder_toggle_${item.key}")
                        )
                    }

                    if (!isFolder) {
                        // Fixed Width (90.dp) for Quantity Controller / Add to Cart
                        Box(
                            modifier = Modifier.width(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cartQuantity > 0) {
                                // Capsule gesture quantity controller
                                var dragAccumulator by remember { mutableFloatStateOf(0f) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                                        onUpdateQuantity(1)
                                                        dragAccumulator = 0f
                                                    } else if (dragAccumulator < -30f) {
                                                        onUpdateQuantity(-1)
                                                        dragAccumulator = 0f
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("qty_capsule_${item.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(-1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = WayStockTextMain,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Text(
                                            text = cartQuantity.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WayStockDark
                                        )

                                        IconButton(
                                            onClick = { onUpdateQuantity(1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = WayStockTextMain,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = onAddToCart,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, WayStockBorder, RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .testTag("add_to_cart_${item.key}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = "Add to Cart",
                                        tint = WayStockTextSec,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Unit Dropdown chip (Fixed 76.dp Width for Strict Vertical Grid Alignment)
                        val unitsList = remember(item.allowedUnitsCsv) {
                            item.allowedUnitsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        val currentUnitDisplay = if (cartQuantity > 0) cartUnit else item.currentUnit

                        Box {
                            Surface(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { unitMenuExpanded = true }
                                    .border(1.dp, WayStockBorder, RoundedCornerShape(8.dp))
                                    .testTag("unit_chip_${item.key}"),
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
                                        text = currentUnitDisplay,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WayStockDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = WayStockTextSec,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                                unitsList.forEach { unit ->
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
                                                        onClick = { onDeleteCustomUnit(unit) },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete Unit",
                                                            tint = WayStockDanger,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            unitMenuExpanded = false
                                            onUnitSelected(unit)
                                        }
                                    )
                                }

                                if (isAdminMode) {
                                    HorizontalDivider(color = WayStockBorder)
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        OutlinedTextField(
                                            value = newUnitInputText,
                                            onValueChange = { newUnitInputText = it },
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
                                                        if (newUnitInputText.isNotBlank()) {
                                                            onAddCustomUnit(newUnitInputText)
                                                            newUnitInputText = ""
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
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
        }
    }
}
