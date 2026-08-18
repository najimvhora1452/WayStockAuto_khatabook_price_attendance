package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InventoryItemEntity
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun PriceUpdateDialog(
    item: InventoryItemEntity,
    onDismiss: () -> Unit,
    onSavePrice: (newMrp: Double, newWholesale: Double, newCost: Double, note: String) -> Unit
) {
    var mrpText by remember { mutableStateOf(if (item.mrp > 0) item.mrp.toString() else "") }
    var wholesaleText by remember { mutableStateOf(if (item.wholesalePrice > 0) item.wholesalePrice.toString() else "") }
    var costText by remember { mutableStateOf(if (item.costPrice > 0) item.costPrice.toString() else "") }
    var noteText by remember { mutableStateOf(item.priceNote) }

    val parsedMrp = mrpText.toDoubleOrNull() ?: 0.0
    val parsedWholesale = wholesaleText.toDoubleOrNull() ?: 0.0
    val parsedCost = costText.toDoubleOrNull() ?: 0.0

    // Retail profit calculation
    val retailProfit = parsedMrp - parsedCost
    val retailMarginPercent = if (parsedCost > 0) (retailProfit / parsedCost) * 100 else 0.0

    // Wholesale profit calculation
    val wholesaleProfit = parsedWholesale - parsedCost
    val wholesaleMarginPercent = if (parsedCost > 0) (wholesaleProfit / parsedCost) * 100 else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("price_update_dialog"),
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
                            color = Color(0xFFFFEDD5),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Sell,
                                    contentDescription = "Price Edit",
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Update Item Price",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                item.displayName.ifBlank { item.name },
                                fontSize = 13.sp,
                                color = WayStockPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Inputs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item Unit Info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Standard Billing Unit:", fontSize = 12.sp, color = WayStockTextSec)
                            Surface(
                                color = WayStockPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Per ${item.currentUnit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 1. Buying / Cost Price
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Purchase / Cost Price (Buying ₹)") },
                        placeholder = { Text("e.g. 15.00") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WayStockTextSec) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WayStockDark,
                            unfocusedTextColor = WayStockDark,
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_cost_price")
                    )

                    // 2. Wholesale Rate
                    OutlinedTextField(
                        value = wholesaleText,
                        onValueChange = { wholesaleText = it },
                        label = { Text("Wholesale Rate (B2B Selling ₹)") },
                        placeholder = { Text("e.g. 18.00") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WayStockDark,
                            unfocusedTextColor = WayStockDark,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_wholesale_price")
                    )

                    // 3. Retail MRP
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = { mrpText = it },
                        label = { Text("Retail MRP (Max Price ₹)") },
                        placeholder = { Text("e.g. 20.00") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16A34A)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WayStockDark,
                            unfocusedTextColor = WayStockDark,
                            focusedBorderColor = Color(0xFF16A34A),
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_mrp_price")
                    )

                    // Reason / Note
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Price Change Note / Reason") },
                        placeholder = { Text("e.g. Company tax hike, Festival discount") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WayStockDark,
                            unfocusedTextColor = WayStockDark,
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_price_note")
                    )

                    // Live Profit & Margin Preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "📊 Live Margin & Profit Calculator",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Wholesale Profit
                                Column {
                                    Text("Wholesale Margin", fontSize = 11.sp, color = WayStockTextSec)
                                    Text(
                                        "₹${String.format(Locale.US, "%.2f", wholesaleProfit)} / unit",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wholesaleProfit >= 0) Color(0xFF0284C7) else WayStockDanger
                                    )
                                    Text(
                                        "${String.format(Locale.US, "%.1f", wholesaleMarginPercent)}% profit",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF0284C7)
                                    )
                                }

                                // Retail Profit
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Retail MRP Margin", fontSize = 11.sp, color = WayStockTextSec)
                                    Text(
                                        "₹${String.format(Locale.US, "%.2f", retailProfit)} / unit",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (retailProfit >= 0) Color(0xFF16A34A) else WayStockDanger
                                    )
                                    Text(
                                        "${String.format(Locale.US, "%.1f", retailMarginPercent)}% profit",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = WayStockTextSec)
                    }

                    Button(
                        onClick = {
                            onSavePrice(parsedMrp, parsedWholesale, parsedCost, noteText.trim())
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .testTag("save_price_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Log Price", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
