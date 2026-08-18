package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
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
import com.example.data.KhataCustomerEntity
import com.example.ui.theme.*

@Composable
fun AddEditKhataCustomerDialog(
    customerToEdit: KhataCustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, phone: String, address: String, customerType: String, initialBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(customerToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var customerType by remember { mutableStateOf(customerToEdit?.customerType ?: "Customer") }
    var initialBalanceStr by remember { mutableStateOf(if (customerToEdit == null) "" else customerToEdit.balance.toString()) }
    var balanceType by remember { mutableStateOf(if ((customerToEdit?.balance ?: 0.0) >= 0) "GET" else "GIVE") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
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
                            text = if (customerToEdit == null) "Add Customer / Party" else "Edit Party Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark
                        )
                        Text(
                            text = "Khata Book Account Book",
                            fontSize = 12.sp,
                            color = WayStockTextSec
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Party Type Selector (Customer vs Supplier)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Customer", "Supplier", "Staff Khata").forEach { type ->
                        val isSelected = customerType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { customerType = type },
                            color = if (isSelected) WayStockPrimary else WayStockSelectedBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = type,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else WayStockTextMain,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Party / Customer Name *") },
                    placeholder = { Text("e.g. Ramesh Bhai, Patel Kirana") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WayStockPrimary) },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name is required", color = Color(0xFFDC2626)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("khata_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (for WhatsApp / SMS)") },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WayStockPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Address Input
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Market Location (Optional)") },
                    placeholder = { Text("e.g. Shop No. 12, Station Road") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = WayStockPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    )
                )

                if (customerToEdit == null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Opening Balance (Shuruat Ka Hisab)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { balanceType = "GET" },
                            color = if (balanceType == "GET") Color(0xFF16A34A).copy(alpha = 0.15f) else WayStockSelectedBg,
                            shape = RoundedCornerShape(10.dp),
                            border = if (balanceType == "GET") androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A)) else null
                        ) {
                            Text(
                                text = "Lene Baaki (+ Get)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (balanceType == "GET") Color(0xFF16A34A) else WayStockTextMain,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { balanceType = "GIVE" },
                            color = if (balanceType == "GIVE") Color(0xFFDC2626).copy(alpha = 0.15f) else WayStockSelectedBg,
                            shape = RoundedCornerShape(10.dp),
                            border = if (balanceType == "GIVE") androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)) else null
                        ) {
                            Text(
                                text = "Dene Baaki (- Give)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (balanceType == "GIVE") Color(0xFFDC2626) else WayStockTextMain,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WayStockPrimary,
                            unfocusedBorderColor = WayStockBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            val rawAmt = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            val finalBal = if (balanceType == "GET") rawAmt else -rawAmt
                            onSave(customerToEdit?.id, name, phone, address, customerType, finalBal)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_khata_customer_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Party", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
