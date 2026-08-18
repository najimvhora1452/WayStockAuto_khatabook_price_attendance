package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun AddNewPriceItemDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, unit: String, mrp: Double, wholesale: Double, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(existingCategories.firstOrNull() ?: "General") }
    var unit by remember { mutableStateOf("Box") }
    var mrpText by remember { mutableStateOf("") }
    var wholesaleText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var customCategoryText by remember { mutableStateOf("") }
    var isCreatingNewCategory by remember { mutableStateOf(existingCategories.isEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_new_price_item_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Item & Set Price",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Name
                Text("Item Name *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Lays Magic Masala", fontSize = 13.sp, color = WayStockTextSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_item_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category
                Text("Category / Folder *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                Spacer(modifier = Modifier.height(4.dp))
                if (isCreatingNewCategory || existingCategories.isEmpty()) {
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        placeholder = { Text("e.g. Snacks, Beverages, etc.", fontSize = 13.sp, color = WayStockTextSec) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (existingCategories.isNotEmpty()) {
                        TextButton(
                            onClick = { isCreatingNewCategory = false },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Choose from existing categories", fontSize = 11.5.sp, color = WayStockPrimary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(category, fontSize = 13.sp, color = WayStockDark)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                existingCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { isCreatingNewCategory = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+ New", fontSize = 12.sp, color = WayStockPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default Unit
                Text("Packaging Unit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    placeholder = { Text("e.g. Box, Packet, Can, Bottle", fontSize = 13.sp, color = WayStockTextSec) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = WayStockBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Price Inputs in 3 Columns
                Text("Price Details (₹)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MRP
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MRP (₹)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WayStockTextSec)
                        Spacer(modifier = Modifier.height(3.dp))
                        OutlinedTextField(
                            value = mrpText,
                            onValueChange = { mrpText = it },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Wholesale Price
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wholesale (₹)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(3.dp))
                        OutlinedTextField(
                            value = wholesaleText,
                            onValueChange = { wholesaleText = it },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buying / Cost Price
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cost (₹)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WayStockTextSec)
                        Spacer(modifier = Modifier.height(3.dp))
                        OutlinedTextField(
                            value = costText,
                            onValueChange = { costText = it },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = WayStockTextSec)
                    }

                    Button(
                        onClick = {
                            val finalCat = if (isCreatingNewCategory) customCategoryText.trim() else category.trim()
                            val mrpVal = mrpText.toDoubleOrNull() ?: 0.0
                            val wsVal = wholesaleText.toDoubleOrNull() ?: 0.0
                            val costVal = costText.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onSave(name.trim(), finalCat.ifBlank { "General" }, unit.trim().ifBlank { "Box" }, mrpVal, wsVal, costVal)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
