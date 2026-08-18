package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KhataCustomerEntity
import com.example.data.KhataTransactionEntity
import com.example.ui.WayStockViewModel
import com.example.ui.dialogs.AddEditKhataCustomerDialog
import com.example.ui.dialogs.AddKhataTransactionDialog
import com.example.ui.theme.*
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataBookScreen(
    viewModel: WayStockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allCustomers by viewModel.allKhataCustomers.collectAsState()
    val allTransactions by viewModel.allKhataTransactions.collectAsState()
    val selectedCustomerTransactions by viewModel.selectedCustomerTransactions.collectAsState()
    val inventoryItems by viewModel.allInventoryItems.collectAsState()
    val context = LocalContext.current

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Map customerId -> List of recent distinct items/notes purchased by that customer
    val customerRecentItemsMap = remember(allTransactions) {
        val map = mutableMapOf<String, List<Pair<String, Double>>>()
        allCustomers.forEach { cust ->
            val custTxns = allTransactions.filter { it.customerId == cust.id && it.type == "GAVE" }
            val distinctPurchases = mutableListOf<Pair<String, Double>>()
            custTxns.forEach { t ->
                val rawNote = t.note.trim()
                if (rawNote.isNotBlank()) {
                    // Extract base name e.g. "2x Thumsup" -> "Thumsup"
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

    // Summary calculations
    val totalLeneBaaki = remember(allCustomers) {
        allCustomers.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val totalDeneBaaki = remember(allCustomers) {
        allCustomers.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    }
    val advanceCustomersCount = remember(allCustomers) {
        allCustomers.count { it.balance < 0 }
    }
    val dueCustomersCount = remember(allCustomers) {
        allCustomers.count { it.balance > 0 }
    }

    // Filter customers based on search and selected filter tab
    val filteredCustomers = remember(allCustomers, uiState.khataSearchQuery, uiState.khataFilterType) {
        allCustomers.filter { customer ->
            val matchQuery = if (uiState.khataSearchQuery.isBlank()) true else {
                customer.name.contains(uiState.khataSearchQuery, ignoreCase = true) ||
                        customer.phone.contains(uiState.khataSearchQuery, ignoreCase = true) ||
                        customer.address.contains(uiState.khataSearchQuery, ignoreCase = true)
            }
            val matchType = when (uiState.khataFilterType) {
                "CUSTOMERS" -> customer.customerType == "Customer"
                "SUPPLIERS" -> customer.customerType == "Supplier"
                "ADVANCE" -> customer.balance < 0.0 // Customers with advance balance deposit
                "PENDING" -> customer.balance > 0.0 // Customers with unpaid udhar
                else -> true
            }
            matchQuery && matchType
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WayStockBg)
            .testTag("khata_book_screen")
    ) {
        // Main Customers List View OR Customer Ledger Detail View
        if (uiState.isKhataDetailOpen && uiState.selectedKhataCustomer != null) {
            CustomerLedgerDetailView(
                customer = uiState.selectedKhataCustomer!!,
                transactions = selectedCustomerTransactions,
                todayDateStr = todayDateStr,
                onBack = { viewModel.closeKhataDetail() },
                onAddGave = { viewModel.openAddKhataTxn("GAVE") },
                onAddGot = { viewModel.openAddKhataTxn("GOT") },
                onEditCustomer = { viewModel.openAddKhataCustomer(uiState.selectedKhataCustomer) },
                onDeleteCustomer = { viewModel.deleteKhataCustomer(uiState.selectedKhataCustomer!!.id) },
                onDeleteTransaction = { txn -> viewModel.deleteKhataTransaction(txn) },
                onSendReminder = { cust ->
                    if (cust.phone.isNotBlank()) {
                        val cleanPhone = cust.phone.replace(" ", "").replace("-", "")
                        val message = if (cust.balance > 0) {
                            "Namaste ${cust.name} ji, aapke WayStock account me ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(cust.balance))} ka hisab (Udhar) baaki he. Kripya payment clear karein. Dhanyawad!"
                        } else {
                            "Namaste ${cust.name} ji, aapka advance balance ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(cust.balance))} WayStock khata me available he. Dhanyawad!"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$cleanPhone")
                                putExtra("sms_body", message)
                            }
                            context.startActivity(smsIntent)
                        }
                    } else {
                        viewModel.showAlert("⚠️ Please add customer phone number first", "info")
                    }
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Summary Dashboard
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📒 Khata & Udhar Ledger",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WayStockDark
                                )
                                Text(
                                    text = "Frequent Retail Udhar & Advance Accounts",
                                    fontSize = 11.5.sp,
                                    color = WayStockTextSec
                                )
                            }

                            Button(
                                onClick = { viewModel.openAddKhataCustomer() },
                                colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_customer_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Customer", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Permanent Sticky Notification Quick Entry Toggle Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.isKhataStickyNotificationEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isKhataStickyNotificationEnabled) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (uiState.isKhataStickyNotificationEnabled) Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(if (uiState.isKhataStickyNotificationEnabled) "🔔" else "🔕", fontSize = 16.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Permanent Sticky Khata Bar",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WayStockDark
                                            )
                                            if (uiState.isKhataStickyNotificationEnabled) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF16A34A),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Search customer & make 1-tap entries right from phone notification shade",
                                            fontSize = 10.sp,
                                            color = WayStockTextSec,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isKhataStickyNotificationEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.toggleKhataStickyNotification(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF16A34A)
                                    ),
                                    modifier = Modifier.testTag("sticky_khata_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Summary Cards: Udhar Lene Baaki vs Advance Deposits / Dene Baaki
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // You will get (Red / Udhar Lene Baaki)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Udhar Lene Baaki",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalLeneBaaki)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB91C1C),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$dueCustomersCount parties due",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }

                            // Advance / You will give (Green / Advance Jama Deposit)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Advance Deposit",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalDeneBaaki)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$advanceCustomersCount prepaid accounts",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = uiState.khataSearchQuery,
                            onValueChange = { viewModel.setKhataSearchQuery(it) },
                            placeholder = { Text("Search customer by name, mobile number...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WayStockPrimary) },
                            trailingIcon = {
                                if (uiState.khataSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setKhataSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = WayStockTextSec)
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

                        // Filter Chips (All, Advance Accounts, Unpaid Udhar, Suppliers)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filterOptions = listOf(
                                "ALL" to "All (${allCustomers.size})",
                                "ADVANCE" to "💰 Advance Wallet ($advanceCustomersCount)",
                                "PENDING" to "🔴 Unpaid Udhar ($dueCustomersCount)",
                                "CUSTOMERS" to "Regular Customers",
                                "SUPPLIERS" to "Suppliers"
                            )
                            items(filterOptions) { (typeKey, label) ->
                                val isSelected = uiState.khataFilterType == typeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setKhataFilterType(typeKey) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WayStockPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Customers Ledger List
                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.khataSearchQuery.isNotBlank()) "No matching customer found" else "No Customer Khata added yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Customer' to start fast udhar & advance daily ledger tracking",
                                fontSize = 12.sp,
                                color = WayStockTextSec,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val recentPurchases = customerRecentItemsMap[customer.id] ?: emptyList()
                            KhataCustomerRowCard(
                                customer = customer,
                                recentPurchases = recentPurchases,
                                onClick = { viewModel.openKhataDetail(customer) },
                                onQuickAddPurchase = { itemName, itemPrice ->
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
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add / Edit Customer Dialog
        if (uiState.isAddKhataCustomerOpen) {
            AddEditKhataCustomerDialog(
                customerToEdit = uiState.customerToEdit,
                onDismiss = { viewModel.closeAddKhataCustomer() },
                onSave = { id, name, phone, address, type, bal ->
                    viewModel.saveKhataCustomer(id, name, phone, address, type, bal)
                }
            )
        }

        // Add Transaction Dialog (Udhar Diya / Jama Liya)
        if (uiState.isAddKhataTxnOpen && uiState.selectedKhataCustomer != null) {
            AddKhataTransactionDialog(
                customer = uiState.selectedKhataCustomer!!,
                initialType = uiState.khataTxnTypeToAdd,
                inventoryItems = inventoryItems,
                onDismiss = { viewModel.closeAddKhataTxn() },
                onSave = { amt, type, note, mode, bill, date ->
                    viewModel.addKhataTransaction(
                        customerId = uiState.selectedKhataCustomer!!.id,
                        customerName = uiState.selectedKhataCustomer!!.name,
                        amount = amt,
                        type = type,
                        note = note,
                        paymentMode = mode,
                        billNumber = bill,
                        date = date
                    )
                }
            )
        }
    }
}

/**
 * Streamlined Fast Retail Customer Row Card in Khata List
 * Displays ONLY: Customer Name + Front Wallet Balance,
 * plus 1-tap Quick Repeat Purchase Chips right underneath.
 */
@Composable
private fun KhataCustomerRowCard(
    customer: KhataCustomerEntity,
    recentPurchases: List<Pair<String, Double>>,
    onClick: () -> Unit,
    onQuickAddPurchase: (itemName: String, itemPrice: Double) -> Unit
) {
    val isAdvance = customer.balance < 0
    val isUdharDue = customer.balance > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("khata_party_${customer.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Main Top Row: Customer Name (Left) <---> Wallet Balance (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar + Customer Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isAdvance -> Color(0xFF16A34A)
                                    isUdharDue -> Color(0xFFDC2626)
                                    else -> Color(0xFF2563EB)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Prominent Wallet Balance
                val balance = customer.balance
                val formattedAmt = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(balance))}"

                Surface(
                    color = when {
                        isAdvance -> Color(0xFFDCFCE7)
                        isUdharDue -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF1F5F9)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when {
                            isAdvance -> Color(0xFF86EFAC)
                            isUdharDue -> Color(0xFFFCA5A5)
                            else -> Color(0xFFCBD5E1)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                isAdvance -> "Advance:"
                                isUdharDue -> "Udhar:"
                                else -> "Wallet:"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isAdvance -> Color(0xFF166534)
                                isUdharDue -> Color(0xFF991B1B)
                                else -> WayStockTextSec
                            }
                        )
                        Text(
                            text = formattedAmt,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isAdvance -> Color(0xFF16A34A)
                                isUdharDue -> Color(0xFFDC2626)
                                else -> WayStockDark
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick Repeat Purchases Section (Instant 1-Tap Entry with Auto 1-Min Multiplier)
            val defaultFallbackItems = remember {
                listOf(
                    Pair("Thumsup", 20.0),
                    Pair("Tea / Chai", 10.0),
                    Pair("Cigarette", 18.0),
                    Pair("Water Bottle", 20.0)
                )
            }
            val displayItems = if (recentPurchases.isNotEmpty()) recentPurchases else defaultFallbackItems

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Quick Entry:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WayStockTextSec,
                    modifier = Modifier.padding(end = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(displayItems) { item ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onQuickAddPurchase(item.first, item.second)
                                }
                                .testTag("quick_item_${customer.id}_${item.first}"),
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "+1",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WayStockPrimary
                                )
                                Text(
                                    text = item.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WayStockDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "₹${item.second.toInt()}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customer Detailed Transaction Ledger Page
 */
@Composable
private fun CustomerLedgerDetailView(
    customer: KhataCustomerEntity,
    transactions: List<KhataTransactionEntity>,
    todayDateStr: String,
    onBack: () -> Unit,
    onAddGave: () -> Unit,
    onAddGot: () -> Unit,
    onEditCustomer: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onDeleteTransaction: (KhataTransactionEntity) -> Unit,
    onSendReminder: (KhataCustomerEntity) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Today's summary for this specific customer
    val todayTransactions = remember(transactions, todayDateStr) {
        transactions.filter { it.date == todayDateStr }
    }
    val todayTotalUdhar = remember(todayTransactions) {
        todayTransactions.filter { it.type == "GAVE" }.sumOf { it.amount }
    }
    val todayTotalJama = remember(todayTransactions) {
        todayTransactions.filter { it.type == "GOT" }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WayStockBg)
    ) {
        // Detail Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WayStockDark)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = customer.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (customer.phone.isNotBlank()) customer.phone else customer.customerType,
                                fontSize = 11.5.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (customer.phone.isNotBlank()) {
                            IconButton(onClick = { onSendReminder(customer) }) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp Reminder", tint = Color(0xFF16A34A))
                            }
                        }
                        IconButton(onClick = onEditCustomer) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Party", tint = WayStockPrimary)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Party", tint = Color(0xFFDC2626))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Outstanding Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        customer.balance > 0 -> Color(0xFFFEE2E2)
                        customer.balance < 0 -> Color(0xFFDCFCE7)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when {
                                    customer.balance > 0 -> "🔴 Udhar Baaki (Aapko Lene He)"
                                    customer.balance < 0 -> "🟢 Advance Deposit (Customer Wallet)"
                                    else -> "Hisab Barabar (Settled)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    customer.balance > 0 -> Color(0xFF991B1B)
                                    customer.balance < 0 -> Color(0xFF166534)
                                    else -> WayStockTextMain
                                }
                            )
                            if (customer.address.isNotBlank()) {
                                Text(
                                    text = "📍 ${customer.address}",
                                    fontSize = 11.sp,
                                    color = WayStockTextSec
                                )
                            }
                        }

                        Text(
                            text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(customer.balance))}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                customer.balance > 0 -> Color(0xFFB91C1C)
                                customer.balance < 0 -> Color(0xFF15803D)
                                else -> WayStockDark
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Aaj Ka Hisaab Bar (If customer visited today)
                if (todayTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = WayStockBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 Aaj Ka Hisaab (${todayTransactions.size} visits):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (todayTotalUdhar > 0) {
                                    Text(
                                        text = "Udhar: ₹${todayTotalUdhar.toInt()}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                if (todayTotalJama > 0) {
                                    Text(
                                        text = "Jama: ₹${todayTotalJama.toInt()}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions Table Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE2E8F0)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ENTRY & VIVARAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Text("UDHAR (DIYA)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Text("JAMA (MILA)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
            }
        }

        // Transactions Entries List
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions recorded yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    Text("Tap 🔴 Udhar Diya or 🟢 Jama Liya below to add instant entries", fontSize = 12.sp, color = WayStockTextSec, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(transactions, key = { it.id }) { txn ->
                    KhataTransactionCard(
                        txn = txn,
                        isToday = txn.date == todayDateStr,
                        onDelete = { onDeleteTransaction(txn) }
                    )
                }
            }
        }

        // Bottom Action Bar: [Udhar Diya 🔴] vs [Jama Mila / Advance 🟢]
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddGave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_you_gave")
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔴 Udhar Diya", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }

                Button(
                    onClick = onAddGot,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_you_got")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🟢 Jama / Advance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Customer?") },
            text = { Text("Are you sure you want to delete '${customer.name}' and all associated ledger transactions?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCustomer()
                    }
                ) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual Transaction Entry Card with Timestamp & Today indicator
 */
@Composable
private fun KhataTransactionCard(
    txn: KhataTransactionEntity,
    isToday: Boolean = false,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (txn.note.isNotBlank()) txn.note else (if (txn.type == "GAVE") "Maal / Udhar Entry" else "Payment Received"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WayStockDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isToday) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${txn.date} • ${txn.time}",
                        fontSize = 10.5.sp,
                        color = WayStockTextSec
                    )
                    if (txn.billNumber.isNotBlank()) {
                        Text(
                            text = "Token: #${txn.billNumber}",
                            fontSize = 10.5.sp,
                            color = WayStockPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        color = WayStockBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = txn.paymentMode,
                            fontSize = 9.sp,
                            color = WayStockTextSec,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val formattedAmt = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)}"

                if (txn.type == "GAVE") {
                    Text(
                        text = formattedAmt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626),
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = formattedAmt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16A34A),
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Delete Entry", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to remove this entry of ₹${txn.amount}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
