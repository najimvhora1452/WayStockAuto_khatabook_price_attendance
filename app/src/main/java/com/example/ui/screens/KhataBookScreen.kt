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
                            "Hello ${cust.name}, this is a reminder from WayStock regarding your outstanding balance of ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(cust.balance))}. Please clear the balance at your earliest convenience. Thank you!"
                        } else {
                            "Hello ${cust.name}, your advance balance on WayStock is ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(cust.balance))}. Thank you!"
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
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ledger Accounts",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockDark
                                )
                                Text(
                                    text = "Customer & Supplier Balance Tracker",
                                    fontSize = 11.sp,
                                    color = WayStockTextSec
                                )
                            }

                            Button(
                                onClick = { viewModel.openAddKhataCustomer() },
                                colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("add_customer_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Summary Cards: Total Due (-) vs Advance Deposit (+)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Due (-)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Total Due (-)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                    Text(
                                        text = "-₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalLeneBaaki)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB91C1C),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$dueCustomersCount due",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }

                            // Advance Deposit (+)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Advance Deposit (+)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Text(
                                        text = "+₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalDeneBaaki)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$advanceCustomersCount prepaid",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = uiState.khataSearchQuery,
                            onValueChange = { viewModel.setKhataSearchQuery(it) },
                            placeholder = { Text("Search by name, phone or address...", fontSize = 12.5.sp, color = WayStockTextSec) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (uiState.khataSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setKhataSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = WayStockTextSec, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WayStockPrimary,
                                unfocusedBorderColor = WayStockBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Filter Chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filterOptions = listOf(
                                "ALL" to "All (${allCustomers.size})",
                                "PENDING" to "Due (-$dueCustomersCount)",
                                "ADVANCE" to "Advance (+$advanceCustomersCount)",
                                "CUSTOMERS" to "Customers",
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
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.khataSearchQuery.isNotBlank()) "No matching accounts found" else "No Ledger Accounts Added",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Account' above to start tracking credit & advance balances.",
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        paymentMode = "Credit",
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

                // Right: Prominent Signed Balance
                val balance = customer.balance
                val isAdvance = balance < 0
                val isUdharDue = balance > 0
                val formattedAmt = when {
                    isAdvance -> "+₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(balance))}"
                    isUdharDue -> "-₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(balance))}"
                    else -> "₹0"
                }

                Surface(
                    color = when {
                        isAdvance -> Color(0xFFDCFCE7)
                        isUdharDue -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF1F5F9)
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when {
                            isAdvance -> Color(0xFF86EFAC)
                            isUdharDue -> Color(0xFFFCA5A5)
                            else -> Color(0xFFCBD5E1)
                        }
                    )
                ) {
                    Text(
                        text = formattedAmt,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isAdvance -> Color(0xFF15803D)
                            isUdharDue -> Color(0xFFB91C1C)
                            else -> WayStockDark
                        },
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Quick Repeat Purchases Section (Only if previous items exist)
            if (recentPurchases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick +1:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WayStockTextSec,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(recentPurchases) { item ->
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
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+1",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WayStockPrimary
                                    )
                                    Text(
                                        text = item.first,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WayStockDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "₹${item.second.toInt()}",
                                        fontSize = 10.sp,
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
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (customer.phone.isNotBlank()) customer.phone else customer.customerType,
                                fontSize = 11.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (customer.phone.isNotBlank()) {
                            IconButton(onClick = { onSendReminder(customer) }) {
                                Icon(Icons.Default.Send, contentDescription = "Send Reminder", tint = Color(0xFF16A34A))
                            }
                        }
                        IconButton(onClick = onEditCustomer) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Account", tint = WayStockPrimary)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = Color(0xFFDC2626))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Current Outstanding Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        customer.balance > 0 -> Color(0xFFFEE2E2)
                        customer.balance < 0 -> Color(0xFFDCFCE7)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when {
                                    customer.balance > 0 -> "🔴 Amount Due (-)"
                                    customer.balance < 0 -> "🟢 Advance Deposit (+)"
                                    else -> "Settled (₹0)"
                                },
                                fontSize = 11.5.sp,
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
                                    fontSize = 10.5.sp,
                                    color = WayStockTextSec
                                )
                            }
                        }

                        val signedText = when {
                            customer.balance > 0 -> "-₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(customer.balance))}"
                            customer.balance < 0 -> "+₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(abs(customer.balance))}"
                            else -> "₹0"
                        }
                        Text(
                            text = signedText,
                            fontSize = 18.sp,
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

                // Today's summary bar (If customer visited today)
                if (todayTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = WayStockBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Activity (${todayTransactions.size} entries):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (todayTotalUdhar > 0) {
                                    Text(
                                        text = "Gave: -₹${todayTotalUdhar.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                if (todayTotalJama > 0) {
                                    Text(
                                        text = "Got: +₹${todayTotalJama.toInt()}",
                                        fontSize = 11.sp,
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
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DETAILS", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Text("GAVE (-)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Text("RECEIVED (+)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
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
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WayStockTextSec, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions recorded yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    Text("Tap 'Gave Credit' or 'Received / Advance' below to add entries", fontSize = 11.5.sp, color = WayStockTextSec, textAlign = TextAlign.Center)
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

        // Bottom Action Bar: [Gave Credit (-)] vs [Received / Advance (+)]
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddGave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_you_gave")
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gave Credit (-)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Button(
                    onClick = onAddGot,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_you_got")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Received (+)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
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
                        text = if (txn.note.isNotBlank()) txn.note else (if (txn.type == "GAVE") "Credit Sale" else "Payment Received"),
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
