package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomItemDialog(
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onAddCustomItem: (name: String, category: String, quantity: Int, unit: String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    val categories = remember(availableCategories) {
        if (availableCategories.isNotEmpty()) availableCategories else listOf("General")
    }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "General") }
    var quantity by remember { mutableIntStateOf(1) }
    val unitOptions = listOf("Box", "Packet", "Kg", "Pcs", "Carton", "Can", "Bottle", "Tray", "Bunch", "Pouch")
    var selectedUnit by remember { mutableStateOf("Box") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var isNameError by remember { mutableStateOf(false) }

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

    val handleDismiss: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    val handleSubmit: () -> Unit = {
        val cleanName = itemName.trim()
        if (cleanName.isEmpty()) {
            isNameError = true
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
            onAddCustomItem(cleanName, selectedCategory, quantity, selectedUnit)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = handleDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("add_custom_item_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Custom Item ✍️",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark
                        )
                        Text(
                            text = "Bucket me temporary item jodein",
                            fontSize = 12.sp,
                            color = WayStockTextSec
                        )
                    }
                    IconButton(
                        onClick = handleDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Name Input
                Text(
                    text = "Item Name *",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNameError) WayStockDanger else WayStockDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        if (isNameError && it.isNotBlank()) isNameError = false
                    },
                    placeholder = { Text("e.g. Tata Tea Gold, Parle G...", fontSize = 13.5.sp, color = WayStockTextSec) },
                    singleLine = true,
                    isError = isNameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WayStockDark,
                        unfocusedTextColor = WayStockDark,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = if (isNameError) WayStockDanger else WayStockBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("custom_item_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Dropdown Selection (Strictly available categories)
                Text(
                    text = "Select Category *",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { categoryDropdownExpanded = true }
                            .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp)),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCategory,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WayStockDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WayStockPrimary)
                        }
                    }

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .background(Color.White)
                            .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = WayStockDark)
                                        if (cat == selectedCategory) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity & Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quantity Stepper
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Quantity",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp)),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = WayStockTextMain, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = quantity.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WayStockDark
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = WayStockTextMain, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Unit Dropdown
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unit",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { unitDropdownExpanded = true }
                                    .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp)),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedUnit,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WayStockDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(18.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false },
                                modifier = Modifier
                                    .width(130.dp)
                                    .background(Color.White)
                                    .border(1.dp, WayStockBorder, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                unitOptions.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = WayStockDark) },
                                        onClick = {
                                            selectedUnit = unit
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = handleDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Text("Cancel", color = WayStockTextSec, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = handleSubmit,
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("submit_custom_item_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Text("Add to Bucket 🛒", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
