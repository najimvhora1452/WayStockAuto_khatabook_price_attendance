package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StaffMemberEntity
import com.example.ui.theme.*

@Composable
fun AddEditStaffDialog(
    staffToEdit: StaffMemberEntity?,
    onDismiss: () -> Unit,
    onSaveStaff: (
        name: String,
        role: String,
        phone: String,
        salaryType: String,
        monthlySalary: Double,
        dailyWage: Double,
        advanceBalance: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf(staffToEdit?.name ?: "") }
    var role by remember { mutableStateOf(staffToEdit?.role ?: "Staff") }
    var phone by remember { mutableStateOf(staffToEdit?.phone ?: "") }
    var salaryType by remember { mutableStateOf(staffToEdit?.salaryType ?: "Monthly") }
    var monthlySalaryText by remember { mutableStateOf(if (staffToEdit != null && staffToEdit.monthlySalary > 0) staffToEdit.monthlySalary.toInt().toString() else "15000") }
    var dailyWageText by remember { mutableStateOf(if (staffToEdit != null && staffToEdit.dailyWage > 0) staffToEdit.dailyWage.toInt().toString() else "500") }
    var advanceText by remember { mutableStateOf(if (staffToEdit != null && staffToEdit.advanceBalance > 0) staffToEdit.advanceBalance.toInt().toString() else "0") }

    val isEditing = staffToEdit != null
    val rolePresets = listOf("Staff", "Delivery Boy", "Warehouse Helper", "Dispatch Lead", "Manager")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("add_edit_staff_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "✏️ Edit Staff Member" else "👤 Add New Staff Member",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WayStockPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Role selection chips
                Column {
                    Text("Designation / Role", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockTextSec)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolePresets.take(3).forEach { r ->
                            val selected = role.equals(r, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { role = r },
                                label = { Text(r, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WayStockPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = WayStockPrimary
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolePresets.drop(3).forEach { r ->
                            val selected = role.equals(r, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { role = r },
                                label = { Text(r, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WayStockPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = WayStockPrimary
                                )
                            )
                        }
                    }
                }

                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile / WhatsApp Number (Optional)") },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WayStockPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Salary Type Selection Tabs (Monthly vs Daily)
                Column {
                    Text("Wage / Salary Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockTextSec)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { salaryType = "Monthly" },
                            color = if (salaryType == "Monthly") WayStockPrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "💼 Monthly Fixed Salary",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (salaryType == "Monthly") Color.White else WayStockDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { salaryType = "Daily" },
                            color = if (salaryType == "Daily") WayStockPrimary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⏱️ Daily Wage (Dihadi)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (salaryType == "Daily") Color.White else WayStockDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Salary Input based on Type
                if (salaryType == "Monthly") {
                    OutlinedTextField(
                        value = monthlySalaryText,
                        onValueChange = { monthlySalaryText = it },
                        label = { Text("Monthly Salary (₹ / Month)") },
                        placeholder = { Text("e.g. 15000") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, color = WayStockPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = dailyWageText,
                        onValueChange = { dailyWageText = it },
                        label = { Text("Daily Wage / Rate (₹ / Day)") },
                        placeholder = { Text("e.g. 500") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, color = WayStockPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Advance Balance (Khata / Advance payment)
                OutlinedTextField(
                    value = advanceText,
                    onValueChange = { advanceText = it },
                    label = { Text("Current Advance Balance (₹) (Optional)") },
                    placeholder = { Text("e.g. 0 or 1000") },
                    leadingIcon = { Text(" ➖ ₹ ", fontWeight = FontWeight.Bold, color = WayStockDanger) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = WayStockTextSec)
                    }

                    Button(
                        onClick = {
                            if (name.trim().isNotBlank()) {
                                val mSalary = monthlySalaryText.toDoubleOrNull() ?: 15000.0
                                val dWage = dailyWageText.toDoubleOrNull() ?: 500.0
                                val adv = advanceText.toDoubleOrNull() ?: 0.0
                                onSaveStaff(
                                    name.trim(),
                                    role.trim(),
                                    phone.trim(),
                                    salaryType,
                                    mSalary,
                                    dWage,
                                    adv
                                )
                            }
                        },
                        enabled = name.trim().isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add Staff", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
