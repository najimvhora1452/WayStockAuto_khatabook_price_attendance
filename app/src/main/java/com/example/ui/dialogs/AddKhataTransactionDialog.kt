package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InventoryItemEntity
import com.example.data.KhataCustomerEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun AddKhataTransactionDialog(
    customer: KhataCustomerEntity,
    initialType: String, // "GAVE" or "GOT"
    inventoryItems: List<InventoryItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: String, note: String, paymentMode: String, billNumber: String, date: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var paymentMode by remember {
        mutableStateOf(
            if (initialType == "GAVE") {
                if (customer.balance < 0) "Advance / Wallet Deduct" else "Credit / Udhar"
            } else "Cash"
        )
    }
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var amountError by remember { mutableStateOf(false) }
    var showInventoryPicker by remember { mutableStateOf(false) }

    // Frequently purchased retail items quick chips
    val commonItems = remember {
        listOf(
            "Cigarettes Pack" to 180.0,
            "Cold Drink" to 40.0,
            "Snacks / Namkeen" to 50.0,
            "Tea / Nashta" to 30.0,
            "Milk / Dairy" to 60.0,
            "Grocery Goods" to 250.0
        )
    }

    // Quick cash denominations
    val quickAmounts = remember {
        if (type == "GAVE") listOf(10, 20, 50, 100, 200, 500, 1000)
        else listOf(50, 100, 200, 500, 1000, 2000, 5000)
    }

    val currentEnteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    // Live balance simulation
    val simulatedNewBalance = remember(customer.balance, currentEnteredAmount, type) {
        if (type == "GAVE") {
            customer.balance + currentEnteredAmount
        } else {
            customer.balance - currentEnteredAmount
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
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
                    Column {
                        Text(
                            text = if (type == "GAVE") "⚡ Fast Udhar Diya (You Gave)" else "💰 Advance / Jama Mila (You Got)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = customer.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            if (customer.balance < 0) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Advance: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(customer.balance))}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch between GAVE and GOT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // You Gave (Red)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                type = "GAVE"
                                if (customer.balance < 0) {
                                    paymentMode = "Advance / Wallet Deduct"
                                } else {
                                    paymentMode = "Credit / Udhar"
                                }
                            },
                        color = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFFDC2626).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔴 Aapne Maal/Udhar Diya",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (type == "GAVE") Color.White else Color(0xFFDC2626)
                            )
                            Text(
                                text = if (customer.balance < 0) "Deduct from Advance" else "+ Add to Udhar",
                                fontSize = 10.sp,
                                color = if (type == "GAVE") Color.White.copy(alpha = 0.85f) else Color(0xFFDC2626).copy(alpha = 0.8f)
                            )
                        }
                    }

                    // You Got (Green)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                type = "GOT"
                                if (paymentMode == "Credit / Udhar" || paymentMode == "Advance / Wallet Deduct") paymentMode = "Cash"
                            },
                        color = if (type == "GOT") Color(0xFF16A34A) else Color(0xFF16A34A).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🟢 Aapko Jama/Advance Mila",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (type == "GOT") Color.White else Color(0xFF16A34A)
                            )
                            Text(
                                text = "Deposit / Payment Clear",
                                fontSize = 10.sp,
                                color = if (type == "GOT") Color.White.copy(alpha = 0.85f) else Color(0xFF16A34A).copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Amount Chips
                Text(
                    text = "⚡ Quick Amount (1-Tap Select)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockTextSec
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickAmounts) { amt ->
                        Surface(
                            modifier = Modifier.clickable {
                                val current = amountStr.toDoubleOrNull() ?: 0.0
                                amountStr = if (amountStr.isBlank()) "$amt" else "${(current + amt).toInt()}"
                                amountError = false
                            },
                            color = WayStockBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                        ) {
                            Text(
                                text = "+₹$amt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = false
                    },
                    label = { Text("Enter Amount (₹) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A),
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    trailingIcon = {
                        if (amountStr.isNotBlank()) {
                            IconButton(onClick = { amountStr = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = WayStockTextSec)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = {
                        if (amountError) {
                            Text("Please enter a valid amount", color = Color(0xFFDC2626))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("khata_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A),
                        unfocusedBorderColor = WayStockBorder
                    )
                )

                // Quick Item Description Shortcuts (For frequent items)
                if (type == "GAVE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛒 Quick Items & Inventory",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockTextSec
                        )
                        if (inventoryItems.isNotEmpty()) {
                            TextButton(
                                onClick = { showInventoryPicker = !showInventoryPicker },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp), tint = WayStockPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showInventoryPicker) "Hide Stock" else "Pick from Stock", fontSize = 11.sp, color = WayStockPrimary)
                            }
                        }
                    }

                    if (showInventoryPicker && inventoryItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .padding(vertical = 4.dp),
                            color = WayStockBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                                inventoryItems.filter { it.type == "item" }.take(10).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                note = if (note.isBlank()) item.name else "$note, ${item.name}"
                                                showInventoryPicker = false
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WayStockDark, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(item.currentUnit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                                    }
                                    HorizontalDivider(color = WayStockBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(commonItems) { (itemName, itemPrice) ->
                            Surface(
                                modifier = Modifier.clickable {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = if (amountStr.isBlank()) "$itemPrice" else "${(current + itemPrice).toInt()}"
                                    note = if (note.isBlank()) itemName else "$note, $itemName"
                                    amountError = false
                                },
                                color = WayStockSelectedBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$itemName (₹${itemPrice.toInt()})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WayStockPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Note / Item description
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Details / Item Vivaran") },
                    placeholder = { Text("e.g. 2 Packs Cigarettes, Cold drink, Advance for next week") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = WayStockPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Mode options
                Text(
                    text = "Account / Payment Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = if (type == "GAVE") {
                        if (customer.balance < 0) listOf("Advance / Wallet Deduct", "Credit / Udhar", "Cash")
                        else listOf("Credit / Udhar", "Cash", "Online")
                    } else {
                        listOf("Cash", "UPI / Online", "Advance Deposit")
                    }
                    modes.forEach { mode ->
                        val isSelected = paymentMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentMode = mode },
                            color = if (isSelected) WayStockPrimary else WayStockSelectedBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = mode,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else WayStockTextMain,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bill Number & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text("Bill / Token No.") },
                        placeholder = { Text("e.g. #104") },
                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        )
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        )
                    )
                }

                // Live simulated balance preview
                if (currentEnteredAmount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (simulatedNewBalance > 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (simulatedNewBalance > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New Balance Preview:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                text = if (simulatedNewBalance > 0) "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(simulatedNewBalance)} (Udhar Lene Baaki)"
                                else if (simulatedNewBalance < 0) "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(simulatedNewBalance))} (Advance Remaining)"
                                else "₹0 (Fully Settled)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (simulatedNewBalance > 0) Color(0xFFDC2626) else if (simulatedNewBalance < 0) Color(0xFF16A34A) else WayStockTextSec,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = WayStockTextSec)
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                amountError = true
                                return@Button
                            }
                            onSave(amt, type, note, paymentMode, billNumber, date)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_khata_txn_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (type == "GAVE") "Record Udhar" else "Save Jama",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

