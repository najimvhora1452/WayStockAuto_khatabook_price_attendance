package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdminDeviceEntity
import com.example.data.AdminPermissions
import com.example.ui.theme.*

@Composable
fun UserPermissionMatrixDialog(
    device: AdminDeviceEntity,
    onDismiss: () -> Unit,
    onSavePermissions: (AdminPermissions) -> Unit
) {
    var permissions by remember(device) { mutableStateOf(device.permissions) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("user_permission_dialog"),
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
                            color = WayStockPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Security",
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "User Permissions Matrix",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                "Manage access for Premium Admin",
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

                // User profile card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    device.displayName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                device.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = WayStockDark
                            )
                            Text(
                                device.email,
                                fontSize = 11.sp,
                                color = WayStockTextSec,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "⭐ Premium",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Feature Access Toggles",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )
                Text(
                    "Turn toggles ON to allow this user to view or manage these sections:",
                    fontSize = 11.sp,
                    color = WayStockTextSec
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
                        title = "🏷️ Price & Margin Tracker",
                        description = "View wholesale rates, MRP, buying costs, and profit margin analytics",
                        isChecked = permissions.canAccessPricePage,
                        accentColor = Color(0xFFEA580C),
                        onCheckedChange = { permissions = permissions.copy(canAccessPricePage = it) }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.Inventory2,
                        title = "📦 Inventory Management",
                        description = "Add new items, rename folders, and edit or delete stock",
                        isChecked = permissions.canManageInventory,
                        accentColor = WayStockPrimary,
                        onCheckedChange = { permissions = permissions.copy(canManageInventory = it) }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.MenuBook,
                        title = "💳 Khata Book & Customer Ledger",
                        description = "View customer balances and add or delete ledger transactions",
                        isChecked = permissions.canManageKhata,
                        accentColor = Color(0xFF0284C7),
                        onCheckedChange = { permissions = permissions.copy(canManageKhata = it) }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.Groups,
                        title = "👥 Staff & Attendance Management",
                        description = "Add staff members, record attendance, and view salary logs",
                        isChecked = permissions.canManageStaff,
                        accentColor = Color(0xFF059669),
                        onCheckedChange = { permissions = permissions.copy(canManageStaff = it) }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.Campaign,
                        title = "📢 Broadcast Notifications",
                        description = "Send sticky notifications or global alerts to all app users",
                        isChecked = permissions.canSendBroadcast,
                        accentColor = Color(0xFF7C3AED),
                        onCheckedChange = { permissions = permissions.copy(canSendBroadcast = it) }
                    )

                    PermissionToggleItem(
                        icon = Icons.Default.FileDownload,
                        title = "📊 Export Reports & Backup",
                        description = "Export and download Inventory, Khata, and Staff reports",
                        isChecked = permissions.canExportReports,
                        accentColor = Color(0xFFD97706),
                        onCheckedChange = { permissions = permissions.copy(canExportReports = it) }
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
                        onClick = { onSavePermissions(permissions) },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .testTag("save_permissions_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Permissions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isChecked) accentColor.copy(alpha = 0.05f) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) accentColor.copy(alpha = 0.4f) else WayStockBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = WayStockDark
                    )
                    Text(
                        description,
                        fontSize = 10.5.sp,
                        color = WayStockTextSec,
                        lineHeight = 14.sp
                    )
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}
