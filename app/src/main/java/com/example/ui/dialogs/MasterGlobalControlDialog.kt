package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MasterSecurityConfig
import com.example.ui.theme.*

@Composable
fun MasterGlobalControlDialog(
    config: MasterSecurityConfig,
    onDismiss: () -> Unit,
    onSaveGlobalToggles: (isPriceGlobal: Boolean, isKhataGlobal: Boolean, isStaffGlobal: Boolean, isInvGlobal: Boolean) -> Unit
) {
    var isPriceGlobal by remember(config) { mutableStateOf(config.isPricePageGlobalToPremium) }
    var isKhataGlobal by remember(config) { mutableStateOf(config.isKhataGlobalToPremium) }
    var isStaffGlobal by remember(config) { mutableStateOf(config.isStaffGlobalToPremium) }
    var isInvGlobal by remember(config) { mutableStateOf(config.isInventoryEditGlobalToPremium) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("master_global_control_dialog"),
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
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Super Admin Master Hub",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                "Global Controls & Broadcast Toggles",
                                fontSize = 12.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Owner Identity Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "👑 SUPER ADMIN (OWNER)",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Level 1 • Full Authority",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            config.masterAdminEmail,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Aap yahan se decide kar sakte hain ki kaunse features sabhi Premium admins ko by default dikhenge aur kaunse sirf aapke paas rahenge.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Global Features Broadcast for All Premium Admins",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Toggles
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionToggleItem(
                        icon = Icons.Default.Sell,
                        title = "🏷️ Price Tracker Global Broadcast",
                        description = if (isPriceGlobal) "ON: Sabhi Google Logged-in Premium Users ko Price Page dikhega." else "OFF: Price Page SIRF AAPKE LIYE dikhega (Super Exclusive).",
                        isChecked = isPriceGlobal,
                        accentColor = Color(0xFFEA580C),
                        onCheckedChange = { isPriceGlobal = it }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.MenuBook,
                        title = "💳 Khata Book Access to All Premium",
                        description = if (isKhataGlobal) "ON: Sabhi Premium users customer khata dekh sakte hain." else "OFF: Sirf selected admins jinhe manually allow kiya ho.",
                        isChecked = isKhataGlobal,
                        accentColor = Color(0xFF0284C7),
                        onCheckedChange = { isKhataGlobal = it }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.Groups,
                        title = "👥 Staff & Attendance to All Premium",
                        description = if (isStaffGlobal) "ON: Sabhi Premium users staff attendance mark kar sakte hain." else "OFF: Staff management restricted to Super Admin.",
                        isChecked = isStaffGlobal,
                        accentColor = Color(0xFF059669),
                        onCheckedChange = { isStaffGlobal = it }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.Inventory2,
                        title = "📦 Stock Edit / Delete to All Premium",
                        description = if (isInvGlobal) "ON: Premium Admins stock edit aur new folders add kar sakte hain." else "OFF: View only for others.",
                        isChecked = isInvGlobal,
                        accentColor = WayStockPrimary,
                        onCheckedChange = { isInvGlobal = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
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
                            onSaveGlobalToggles(isPriceGlobal, isKhataGlobal, isStaffGlobal, isInvGlobal)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .testTag("save_global_master_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Global Rules", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
