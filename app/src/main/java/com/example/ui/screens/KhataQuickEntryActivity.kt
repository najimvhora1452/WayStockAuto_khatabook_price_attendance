package com.example.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.KhataCustomerEntity
import com.example.ui.WayStockViewModel
import com.example.ui.dialogs.AddKhataTransactionDialog
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Compact, fast Floating Overlay / Quick Dialog launched directly from Notification
 * Allows store owner to search customer & record instant 1-tap or custom Udhar/Advance entries from anywhere.
 */
class KhataQuickEntryActivity : ComponentActivity() {
    private val viewModel: WayStockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WayStockTheme {
                KhataQuickEntryFloatingContent(
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataQuickEntryFloatingContent(
    viewModel: WayStockViewModel,
    onClose: () -> Unit
) {
    val allCustomers by viewModel.allKhataCustomers.collectAsState()
    val allTransactions by viewModel.allKhataTransactions.collectAsState()
    val inventoryItems by viewModel.allInventoryItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ADVANCE, PENDING
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Map customerId -> List of recent distinct items/notes purchased by that customer
    val customerRecentItemsMap = remember(allTransactions, allCustomers) {
        val map = mutableMapOf<String, List<Pair<String, Double>>>()
        allCustomers.forEach { cust ->
            val custTxns = allTransactions.filter { it.customerId == cust.id && it.type == "GAVE" }
            val distinctPurchases = mutableListOf<Pair<String, Double>>()
            custTxns.forEach { t ->
                val rawNote = t.note.trim()
                if (rawNote.isNotBlank()) {
                    val match = Regex("""^(\d+)\s*[xX×]\s*(.+)$""").find(rawNote)
                    val baseName = match?.groupValues?.get(2)?.trim() ?: rawNote
                    val itemUnitPrice = if (match != null) {
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        if (qty > 0) t.amount / qty else t.amount
                    } else {
                        t.amount
                    }
                    if (distinctPurchases.none { it.first.equals(baseName, ignoreCase = true) }) {
                        distinctPurchases.add(Pair(baseName, itemUnitPrice))
                    }
                }
            }
            map[cust.id] = distinctPurchases.take(4)
        }
        map
    }

    val filteredCustomers = remember(allCustomers, searchQuery, selectedFilter) {
        allCustomers.filter { customer ->
            val matchQuery = if (searchQuery.isBlank()) true else {
                customer.name.contains(searchQuery, ignoreCase = true) ||
                        customer.phone.contains(searchQuery, ignoreCase = true)
            }
            val matchFilter = when (selectedFilter) {
                "ADVANCE" -> customer.balance < 0.0
                "PENDING" -> customer.balance > 0.0
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WayStockPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚡", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Quick Khata Entry",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WayStockDark
                            )
                            Text(
                                text = "Search customer & tap to record entry",
                                fontSize = 11.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = WayStockTextSec,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customer name or phone...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = WayStockTextSec, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder,
                        focusedContainerColor = WayStockBg,
                        unfocusedContainerColor = WayStockBg
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterList = listOf(
                        "ALL" to "All (${allCustomers.size})",
                        "PENDING" to "🔴 Udhar (${allCustomers.count { it.balance > 0 }})",
                        "ADVANCE" to "💰 Advance (${allCustomers.count { it.balance < 0 }})"
                    )
                    filterList.forEach { (key, label) ->
                        val isSel = selectedFilter == key
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFilter = key },
                            color = if (isSel) WayStockPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else WayStockTextSec,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Customers List
                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No customers found.",
                            fontSize = 13.sp,
                            color = WayStockTextSec,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val recentPurchases = customerRecentItemsMap[customer.id] ?: emptyList()
                            QuickFloatingCustomerCard(
                                customer = customer,
                                recentPurchases = recentPurchases,
                                onQuickAdd = { itemName, itemPrice ->
                                    viewModel.addKhataTransaction(
                                        customerId = customer.id,
                                        customerName = customer.name,
                                        amount = itemPrice,
                                        type = "GAVE",
                                        note = "1× $itemName",
                                        paymentMode = "Credit / Udhar",
                                        billNumber = "",
                                        date = todayDateStr
                                    )
                                    Toast.makeText(context, "✅ Added ₹${itemPrice.toInt()} ($itemName) to ${customer.name}", Toast.LENGTH_SHORT).show()
                                },
                                onCustomGave = {
                                    viewModel.openKhataDetail(customer)
                                    viewModel.openAddKhataTxn("GAVE")
                                    onClose()
                                },
                                onCustomGot = {
                                    viewModel.openKhataDetail(customer)
                                    viewModel.openAddKhataTxn("GOT")
                                    onClose()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Footer Button: Open Full App
                Button(
                    onClick = {
                        viewModel.setCurrentTab("khata")
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Full Khata Book Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Individual Compact Customer Card inside Floating Quick Dialog
 */
@Composable
private fun QuickFloatingCustomerCard(
    customer: KhataCustomerEntity,
    recentPurchases: List<Pair<String, Double>>,
    onQuickAdd: (String, Double) -> Unit,
    onCustomGave: () -> Unit,
    onCustomGot: () -> Unit
) {
    val isAdvance = customer.balance < 0
    val isUdharDue = customer.balance > 0
    val balance = customer.balance
    val formattedAmt = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(balance))}"

    val defaultFallbackItems = remember {
        listOf(
            Pair("Thumsup", 20.0),
            Pair("Tea / Chai", 10.0),
            Pair("Cigarette", 18.0),
            Pair("Water Bottle", 20.0)
        )
    }
    val displayItems = if (recentPurchases.isNotEmpty()) recentPurchases else defaultFallbackItems

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Row 1: Name & Wallet Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        color = when {
                            isAdvance -> Color(0xFF16A34A).copy(alpha = 0.15f)
                            isUdharDue -> Color(0xFFDC2626).copy(alpha = 0.15f)
                            else -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = customer.name.take(1).uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isAdvance -> Color(0xFF16A34A)
                                    isUdharDue -> Color(0xFFDC2626)
                                    else -> Color(0xFF2563EB)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = customer.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = when {
                        isAdvance -> Color(0xFFDCFCE7)
                        isUdharDue -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF1F5F9)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${if (isAdvance) "Advance: " else if (isUdharDue) "Udhar: " else "Wallet: "}$formattedAmt",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isAdvance -> Color(0xFF16A34A)
                            isUdharDue -> Color(0xFFDC2626)
                            else -> WayStockDark
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: 1-Tap Quick Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Add:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WayStockTextSec,
                    modifier = Modifier.padding(end = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(displayItems) { item ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQuickAdd(item.first, item.second) },
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("+1", fontSize = 10.sp, fontWeight = FontWeight.Black, color = WayStockPrimary)
                                Text(item.first, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = WayStockDark, maxLines = 1)
                                Text("₹${item.second.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Custom Udhar Diya / Jama Liya buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCustomGave,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Text("+ Udhar Diya", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCustomGot,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Text("+ Jama Liya", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
