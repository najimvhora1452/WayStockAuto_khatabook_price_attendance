package com.example.ui.dialogs

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminAuthManager
import com.example.data.AdminDeviceEntity
import com.example.data.AdminPermissions
import com.example.data.MasterSecurityConfig
import com.example.data.UserRequestedItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSettingsDialog(
    userRequestedItems: List<UserRequestedItemEntity> = emptyList(),
    allAdminDevices: List<AdminDeviceEntity> = emptyList(),
    loggedInAdminEmail: String? = null,
    loggedInAdminName: String? = null,
    isSuperAdmin: Boolean = false,
    isAutoLaunchEnabled: Boolean = false,
    isGoogleAuthLoading: Boolean = false,
    masterPinLastModifiedBy: String = "Master Admin",
    masterPinLastModifiedAt: Long = 0L,
    masterSecurityConfig: MasterSecurityConfig = MasterSecurityConfig(),
    adminAuthManager: AdminAuthManager? = null,
    isStickyBottomMemoBarEnabled: Boolean = true,
    onToggleStickyBottomMemoBar: (Boolean) -> Unit = {},
    isStickyNotificationEnabled: Boolean = false,
    onToggleStickyNotification: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onGoogleLoginSuccess: (String, String) -> Unit = { _, _ -> },
    onGoogleLoginLoading: (Boolean) -> Unit = {},
    onGoogleLogout: () -> Unit = {},
    onToggleAutoLaunch: (Boolean) -> Unit = {},
    onRemoteToggleDevice: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateDevicePermissions: (String, AdminPermissions) -> Unit = { _, _ -> },
    onUpdateMasterGlobalConfig: (isPrice: Boolean, isKhata: Boolean, isStaff: Boolean, isInv: Boolean) -> Unit = { _, _, _, _ -> },
    onDeleteAdminDevice: (String) -> Unit = {},
    onUpdatePassword: (String, String) -> Unit,
    onSendBroadcast: (String) -> Unit,
    onLogoutAdmin: () -> Unit,
    onDeleteRequestedItem: (Long) -> Unit = {},
    onClearAllRequestedItems: () -> Unit = {},
    onAddRequestedToInventory: (String) -> Unit = {},
    onSyncWithCloud: () -> Unit = {},
    onPushToCloud: () -> Unit = {}
) {
    var isSecurityOpen by remember { mutableStateOf(false) }
    var isAutoLaunchSectionOpen by remember { mutableStateOf(true) }
    var isManageDevicesOpen by remember { mutableStateOf(isSuperAdmin) }
    var isCloudSyncSectionOpen by remember { mutableStateOf(true) }
    var isBroadcastOpen by remember { mutableStateOf(false) }
    var isStickyControlsSectionOpen by remember { mutableStateOf(false) }
    var isRequestedItemsOpen by remember { mutableStateOf(userRequestedItems.isNotEmpty()) }

    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var broadcastInput by remember { mutableStateOf("") }

    var deviceForPermissionMatrix by remember { mutableStateOf<AdminDeviceEntity?>(null) }
    var isMasterGlobalControlOpen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val groupedRequestedItems = remember(userRequestedItems) {
        userRequestedItems.groupBy { it.category }
    }

    val formattedDate = remember(masterPinLastModifiedAt) {
        if (masterPinLastModifiedAt > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(masterPinLastModifiedAt))
        } else {
            "Default"
        }
    }

    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var deviceGoogleAccounts by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeviceAccountPicker by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back navigation to return to Admin Home
    BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("setting_section"),
        color = Color(0xFFF8FAFC)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with statusBarsPadding so it never clips into status bar / notch
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WayStockTextMain)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Admin Settings", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            if (isSuperAdmin) {
                                Text("👑 Super Owner Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            } else if (!loggedInAdminEmail.isNullOrBlank()) {
                                Text("⭐ Admin Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WayStockPrimary)
                            }
                        }
                    }

                    // Exit Admin Button in header
                    Surface(
                        color = Color(0xFFFFF1F2),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                        modifier = Modifier.clickable {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onLogoutAdmin()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Exit", tint = WayStockDanger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Exit Admin",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDanger
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // 1. Google Account & Auto-Launch Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAutoLaunchSectionOpen = !isAutoLaunchSectionOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Auth", tint = WayStockPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Auth & Auto-Admin Launch", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            }
                            Icon(
                                imageVector = if (isAutoLaunchSectionOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isAutoLaunchSectionOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (loggedInAdminEmail.isNullOrBlank()) {
                                    // Not logged in
                                    Text(
                                        "Sign in with your Google account to enable cloud sync and auto-admin launch:",
                                        fontSize = 12.sp,
                                        color = WayStockTextSec,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            if (adminAuthManager != null) {
                                                onGoogleLoginLoading(true)
                                                coroutineScope.launch {
                                                    val result = adminAuthManager.signInWithGoogle(context)
                                                    onGoogleLoginLoading(false)
                                                    if (result.isSuccess) {
                                                        val (email, name) = result.getOrThrow()
                                                        onGoogleLoginSuccess(email, name)
                                                    } else {
                                                        // Check if real Google accounts exist on the Android device
                                                        val accounts = adminAuthManager.getDeviceGoogleAccounts(context)
                                                        if (accounts.isNotEmpty()) {
                                                            deviceGoogleAccounts = accounts
                                                            showDeviceAccountPicker = true
                                                        } else {
                                                            val err = result.exceptionOrNull()?.message ?: "Google Play Services not available on this device."
                                                            authErrorMessage = err
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                    ) {
                                        if (isGoogleAuthLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("G", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Sign in with Google", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Direct Local Auto-Launch Toggle (Available even before cloud sign-in)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoLaunchEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoLaunchEnabled) Color(0xFF10B981) else WayStockBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🚀 Auto-Open Admin on Launch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                Text("Directly open Admin portal on app startup", fontSize = 11.sp, color = WayStockTextSec)
                                            }
                                            Switch(
                                                checked = isAutoLaunchEnabled,
                                                onCheckedChange = { onToggleAutoLaunch(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF10B981)
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    // Logged In Status
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(loggedInAdminName ?: "Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                    if (isSuperAdmin) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("👑 SUPER OWNER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                                Text(if (isSuperAdmin) "Super Admin Privileges Active" else "Admin Account Active", fontSize = 12.sp, color = WayStockTextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }

                                            OutlinedButton(
                                                onClick = { onGoogleLogout() },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WayStockDanger),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Auto-Launch Toggle
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoLaunchEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoLaunchEnabled) Color(0xFF10B981) else WayStockBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🚀 Auto-Open Admin on Launch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                Text("Directly open Admin portal on app startup", fontSize = 11.sp, color = WayStockTextSec)
                                            }
                                            Switch(
                                                checked = isAutoLaunchEnabled,
                                                onCheckedChange = { onToggleAutoLaunch(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF10B981)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Master Password Change Card (Strictly Super Admin Only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSecurityOpen = !isSecurityOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Lock, contentDescription = "PIN", tint = WayStockPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🔑 Master Admin PIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                    Text("Last modified: $formattedDate", fontSize = 11.sp, color = WayStockTextSec)
                                }
                            }

                            Icon(
                                imageVector = if (isSecurityOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isSecurityOpen) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                if (loggedInAdminEmail.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            "⚠️ Sign in above to update the Master PIN.",
                                            fontSize = 12.sp,
                                            color = WayStockDanger,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                } else if (!isSuperAdmin) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            "🚫 Access Denied: Only Master Owner can update Master PIN.",
                                            fontSize = 12.sp,
                                            color = WayStockDanger,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                var showOldPin by remember { mutableStateOf(false) }
                                var showNewPin by remember { mutableStateOf(false) }
                                val isAllowedToEdit = isSuperAdmin

                                Text("Current PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = oldPinInput,
                                    onValueChange = { oldPinInput = it },
                                    enabled = isAllowedToEdit,
                                    placeholder = { Text("Enter current PIN", fontSize = 13.sp, color = WayStockTextSec) },
                                    singleLine = true,
                                    visualTransformation = if (showOldPin) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                    trailingIcon = {
                                        IconButton(onClick = { showOldPin = !showOldPin }) {
                                            Icon(
                                                imageVector = if (showOldPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle PIN",
                                                tint = WayStockTextSec
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_old_password")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("New PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { newPinInput = it },
                                    enabled = isAllowedToEdit,
                                    placeholder = { Text("Enter new PIN (min 4 digits)", fontSize = 13.sp, color = WayStockTextSec) },
                                    singleLine = true,
                                    visualTransformation = if (showNewPin) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                    trailingIcon = {
                                        IconButton(onClick = { showNewPin = !showNewPin }) {
                                            Icon(
                                                imageVector = if (showNewPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle PIN",
                                                tint = WayStockTextSec
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_new_password")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onUpdatePassword(oldPinInput, newPinInput)
                                    },
                                    enabled = isAllowedToEdit && oldPinInput.isNotBlank() && newPinInput.length >= 4,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WayStockPrimary,
                                        disabledContainerColor = Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Master PIN to Cloud", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 3. Super Admin Device Manager (Remote Control List)
                if (isSuperAdmin) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isManageDevicesOpen = !isManageDevicesOpen },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = "Devices", tint = WayStockPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("👥 Manage Admin Devices (${allAdminDevices.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                }
                                Icon(
                                    imageVector = if (isManageDevicesOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = WayStockTextSec
                                )
                            }

                            AnimatedVisibility(visible = isManageDevicesOpen) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(
                                        "Registered admin accounts. You can manage permissions, toggle auto-launch, or remove devices:",
                                        fontSize = 11.sp,
                                        color = WayStockTextSec
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (allAdminDevices.isEmpty()) {
                                        Text("No admin devices registered yet.", fontSize = 12.sp, color = WayStockTextSec, modifier = Modifier.padding(vertical = 8.dp))
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            allAdminDevices.forEach { device ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (device.isSuperAdmin) Color(0xFFFEF3C7).copy(alpha = 0.5f) else (if (device.isAutoLaunchEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (device.isSuperAdmin) Color(0xFFFDE68A) else (if (device.isAutoLaunchEnabled) Color(0xFF86EFAC) else WayStockBorder)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (device.isSuperAdmin) {
                                                                isMasterGlobalControlOpen = true
                                                            } else {
                                                                deviceForPermissionMatrix = device
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(device.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                                                if (device.isSuperAdmin) {
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("👑 Owner (Tap for Master Controls)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                                }
                                                            }
                                                            Text(device.email, fontSize = 11.sp, color = WayStockTextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            if (!device.isSuperAdmin) {
                                                                Text("⚙️ Tap to configure page permissions", fontSize = 10.sp, color = WayStockPrimary)
                                                            }
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!device.isSuperAdmin) {
                                                                // Permission Matrix Button
                                                                IconButton(
                                                                    onClick = { deviceForPermissionMatrix = device },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Tune, contentDescription = "Permissions", tint = WayStockPrimary, modifier = Modifier.size(18.dp))
                                                                }

                                                                Switch(
                                                                    checked = device.isAutoLaunchEnabled,
                                                                    onCheckedChange = { onRemoteToggleDevice(device.email, it) },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFF10B981)
                                                                    ),
                                                                    modifier = Modifier.scale(0.85f)
                                                                )

                                                                IconButton(
                                                                    onClick = { onDeleteAdminDevice(device.email) },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = WayStockDanger, modifier = Modifier.size(18.dp))
                                                                }
                                                            } else {
                                                                Surface(
                                                                    color = Color(0xFF1E293B),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    modifier = Modifier.clickable { isMasterGlobalControlOpen = true }
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("Master Controls", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Broadcast Notification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBroadcastOpen = !isBroadcastOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📢 Broadcast Notification", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            Icon(
                                imageVector = if (isBroadcastOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isBroadcastOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                OutlinedTextField(
                                    value = broadcastInput,
                                    onValueChange = { broadcastInput = it },
                                    placeholder = { Text("Type message for users (use @user for name)...", fontSize = 13.sp, color = WayStockTextSec) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = WayStockDark,
                                        unfocusedTextColor = WayStockDark,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = WayStockPrimary,
                                        unfocusedBorderColor = WayStockBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("admin_broadcast_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        onSendBroadcast(broadcastInput)
                                        broadcastInput = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                                ) {
                                    Text("Send Global Alert 🚀", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 5. Sticky Bar & Notification Preferences Card (Dropdown Accordion)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isStickyControlsSectionOpen = !isStickyControlsSectionOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("📌 Sticky Bar & Notification Controls", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                    Text("Quick memo bar & status bar shortcuts", fontSize = 11.sp, color = WayStockTextSec)
                                }
                            }
                            Icon(
                                imageVector = if (isStickyControlsSectionOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Section",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isStickyControlsSectionOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                // Toggle 1: Bottom Quick Memo Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📌 Khata Bottom Quick Memo Bar", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                        Text("Show/hide quick input bar at bottom of customer list", fontSize = 11.sp, color = WayStockTextSec)
                                    }
                                    Switch(
                                        checked = isStickyBottomMemoBarEnabled,
                                        onCheckedChange = onToggleStickyBottomMemoBar,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = WayStockPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Toggle 2: Phone Status Bar Sticky Notification Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🔔 Phone Status Bar Notification", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                        Text("Persistent notification bar for 1-tap entry from anywhere", fontSize = 11.sp, color = WayStockTextSec)
                                    }
                                    Switch(
                                        checked = isStickyNotificationEnabled,
                                        onCheckedChange = onToggleStickyNotification,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Cloud Inventory Sync Card (Firestore stock & appSettings)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCloudSyncSectionOpen = !isCloudSyncSectionOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("☁️ Cloud Inventory Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                    Text("stockmaster-94534 • Realtime Active", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Icon(
                                imageVector = if (isCloudSyncSectionOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Section",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isCloudSyncSectionOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    "App auto-syncs inventory on launch and whenever Admin updates items. You can also trigger manual sync below:",
                                    fontSize = 11.sp,
                                    color = WayStockTextSec
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onSyncWithCloud() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pull Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onPushToCloud() },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WayStockPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WayStockPrimary),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. User Requested Items Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRequestedItemsOpen = !isRequestedItemsOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📥 User Requested Items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                if (userRequestedItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = WayStockPrimary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "${userRequestedItems.size}",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (isRequestedItemsOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = WayStockTextSec
                            )
                        }

                        AnimatedVisibility(visible = isRequestedItemsOpen) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (userRequestedItems.isEmpty()) {
                                    Text(
                                        "No pending custom item requests.",
                                        fontSize = 13.sp,
                                        color = WayStockTextSec,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${userRequestedItems.size} custom items requested by users",
                                            fontSize = 12.sp,
                                            color = WayStockTextSec
                                        )
                                        TextButton(onClick = { onClearAllRequestedItems() }) {
                                            Text("Clear All", fontSize = 12.sp, color = WayStockDanger, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    groupedRequestedItems.forEach { (category, items) ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        category,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = WayStockPrimary
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = WayStockPrimary.copy(alpha = 0.12f),
                                                        modifier = Modifier.clickable {
                                                            val structure = items.joinToString("\n") { "$category>${it.name}" }
                                                            onAddRequestedToInventory(structure)
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Add All", tint = WayStockPrimary, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Add All (${items.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                items.forEach { reqItem ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(reqItem.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                                            Text("By: ${reqItem.requestedBy} • Unit: ${reqItem.unit}", fontSize = 11.sp, color = WayStockTextSec)
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            IconButton(
                                                                onClick = {
                                                                    val singleStructure = "$category>${reqItem.name}"
                                                                    onAddRequestedToInventory(singleStructure)
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                                            }

                                                            IconButton(
                                                                onClick = { onDeleteRequestedItem(reqItem.id) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = WayStockDanger, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Render User Permission Matrix Dialog
        deviceForPermissionMatrix?.let { targetDevice ->
            UserPermissionMatrixDialog(
                device = targetDevice,
                onDismiss = { deviceForPermissionMatrix = null },
                onSavePermissions = { perms ->
                    onUpdateDevicePermissions(targetDevice.email, perms)
                    deviceForPermissionMatrix = null
                }
            )
        }

        // Render Master Global Control Dialog
        if (isMasterGlobalControlOpen) {
            MasterGlobalControlDialog(
                config = masterSecurityConfig,
                onDismiss = { isMasterGlobalControlOpen = false },
                onSaveGlobalToggles = { isPrice, isKhata, isStaff, isInv ->
                    onUpdateMasterGlobalConfig(isPrice, isKhata, isStaff, isInv)
                    isMasterGlobalControlOpen = false
                }
            )
        }

        // Render On-Device Google Account Picker (shows actual Google accounts verified on phone)
        if (showDeviceAccountPicker && deviceGoogleAccounts.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showDeviceAccountPicker = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color.White,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Select Google Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "The following verified Google accounts are signed in on this device. Select one to proceed:",
                            fontSize = 12.sp,
                            color = WayStockTextSec
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        deviceGoogleAccounts.forEach { accEmail ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showDeviceAccountPicker = false
                                        val name = accEmail.substringBefore("@").replace(".", " ").capitalize(Locale.ROOT)
                                        onGoogleLoginSuccess(accEmail, name)
                                    },
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4285F4),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                accEmail.take(1).uppercase(Locale.ROOT),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(accEmail, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                        Text("Device Account", fontSize = 10.5.sp, color = WayStockTextSec)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDeviceAccountPicker = false }) {
                        Text("Cancel", color = WayStockTextSec)
                    }
                }
            )
        }

        // Render Auth Error / Notice Dialog
        authErrorMessage?.let { errText ->
            AlertDialog(
                onDismissRequest = { authErrorMessage = null },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color.White,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign-In Notice", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = errText,
                            fontSize = 12.5.sp,
                            color = WayStockTextSec
                        )
                        Text(
                            text = "Ensure Google Play Services is active on device and Google Sign-in provider is enabled in Firebase Console.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF0284C7)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { authErrorMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            )
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(0.dp) // placeholder helper
)
