package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WayStockViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainInventoryScreen(viewModel: WayStockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val allInventoryItems by viewModel.allInventoryItems.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val userRequestedItems by viewModel.userRequestedItems.collectAsState()
    val allAdminDevices by viewModel.allAdminDevices.collectAsState()
    val masterSecurityConfig by viewModel.masterSecurityConfig.collectAsState()
    val attendanceForSelectedStaff by viewModel.attendanceForSelectedStaff.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Play notification sound when a new broadcast message is received
    LaunchedEffect(uiState.broadcastMessage) {
        uiState.broadcastMessage?.let {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            } catch (_: Exception) {}
        }
    }

    val currentAdminDevice = remember(allAdminDevices, uiState.loggedInAdminEmail) {
        allAdminDevices.find { it.email.equals(uiState.loggedInAdminEmail, ignoreCase = true) }
    }
    val isPriceTabAccessible = uiState.isSuperAdmin || (uiState.isAdminMode && (masterSecurityConfig.isPricePageGlobalToPremium || currentAdminDevice?.permissions?.canAccessPricePage == true))

    // Handle Android system back gesture and back button gracefully for screen-level states
    val isDialogCurrentlyOpen = uiState.isAddModalOpen ||
            uiState.isAdminGatewayOpen ||
            uiState.isAdminSettingsOpen ||
            uiState.isShareAppOpen ||
            uiState.isAddStaffDialogOpen ||
            uiState.isAddKhataCustomerOpen ||
            uiState.isAddKhataTxnOpen

    val isAnyOverlayOrNestedOpen = !isDialogCurrentlyOpen && (
            uiState.isSelectionMode ||
            uiState.isSearchOpen ||
            uiState.isOrderSlipOpen ||
            uiState.isCartOpen ||
            uiState.isStaffDetailOpen ||
            uiState.isKhataDetailOpen ||
            uiState.currentTab != "inventory" ||
            uiState.pathStack.size > 1
    )

    BackHandler(enabled = isAnyOverlayOrNestedOpen) {
        when {
            uiState.isSelectionMode -> viewModel.clearCardSelection()
            uiState.isSearchOpen -> viewModel.setSearchOpen(false)
            uiState.isOrderSlipOpen -> viewModel.setOrderSlipOpen(false)
            uiState.isCartOpen -> viewModel.setCartOpen(false)
            uiState.isStaffDetailOpen -> viewModel.closeStaffDetail()
            uiState.isKhataDetailOpen -> viewModel.closeKhataDetail()
            uiState.currentTab != "inventory" -> viewModel.setCurrentTab("inventory")
            uiState.pathStack.size > 1 -> viewModel.jumpToBreadcrumb(uiState.pathStack.size - 2)
        }
    }

    // Show alert messages in snackbar
    LaunchedEffect(uiState.alertMessage) {
        uiState.alertMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAlert()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("app_bottom_bar")
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == "inventory",
                        onClick = { viewModel.setCurrentTab("inventory") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Inventory"
                            )
                        },
                        label = {
                            Text(
                                text = "Inventory",
                                fontWeight = if (uiState.currentTab == "inventory") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WayStockPrimary,
                            selectedTextColor = WayStockPrimary,
                            indicatorColor = WayStockPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = WayStockTextSec,
                            unselectedTextColor = WayStockTextSec
                        )
                    )

                    // Price Tracker Tab (Shown for Super Admin and Authorized Premium users)
                    if (isPriceTabAccessible) {
                        NavigationBarItem(
                            selected = uiState.currentTab == "price",
                            onClick = { viewModel.setCurrentTab("price") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Sell,
                                    contentDescription = "Price Tracker"
                                )
                            },
                            label = {
                                Text(
                                    text = "Prices",
                                    fontWeight = if (uiState.currentTab == "price") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WayStockPrimary,
                                selectedTextColor = WayStockPrimary,
                                indicatorColor = WayStockPrimary.copy(alpha = 0.12f),
                                unselectedIconColor = WayStockTextSec,
                                unselectedTextColor = WayStockTextSec
                            )
                        )
                    }

                    NavigationBarItem(
                        selected = uiState.currentTab == "attendance",
                        onClick = { viewModel.setCurrentTab("attendance") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Attendance"
                            )
                        },
                        label = {
                            Text(
                                text = "Attendance",
                                fontWeight = if (uiState.currentTab == "attendance") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WayStockPrimary,
                            selectedTextColor = WayStockPrimary,
                            indicatorColor = WayStockPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = WayStockTextSec,
                            unselectedTextColor = WayStockTextSec
                        )
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == "khata",
                        onClick = { viewModel.setCurrentTab("khata") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Khata Book"
                            )
                        },
                        label = {
                            Text(
                                text = "Khata Book",
                                fontWeight = if (uiState.currentTab == "khata") FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WayStockPrimary,
                            selectedTextColor = WayStockPrimary,
                            indicatorColor = WayStockPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = WayStockTextSec,
                            unselectedTextColor = WayStockTextSec
                        )
                    )
                }
            },
            topBar = {
                if (uiState.currentTab == "inventory") {
                    Column {
                        if (uiState.isSelectionMode) {
                            // Selection Action Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("selection_toolbar"),
                                color = Color.White,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.clearCardSelection() },
                                            modifier = Modifier.testTag("cancel_selection_btn")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = WayStockTextSec)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${uiState.selectedCardKeys.size} Selected",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WayStockDark
                                        )
                                    }

                                    val currentFolderItems = allInventoryItems.filter { it.parent == uiState.currentFolder }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val isAllSelected = uiState.selectedCardKeys.size == currentFolderItems.size && currentFolderItems.isNotEmpty()
                                        IconButton(
                                            onClick = {
                                                if (isAllSelected) viewModel.clearCardSelection()
                                                else viewModel.selectAllInCurrentFolder(currentFolderItems)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SelectAll,
                                                contentDescription = "Select All",
                                                tint = WayStockPrimary
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.bulkAddToCartSelected() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ShoppingCart,
                                                contentDescription = "Bulk Add to Cart",
                                                tint = WayStockPrimary
                                            )
                                        }

                                        if (uiState.selectedCardKeys.size == 1) {
                                            val singleKey = uiState.selectedCardKeys.first()
                                            IconButton(
                                                onClick = {
                                                    viewModel.setAddModalOpen(true, editKey = singleKey)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Edit Name",
                                                    tint = WayStockPrimary
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.bulkDeleteSelected() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete Selected",
                                                tint = WayStockDanger
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            HeaderBar(
                                userName = uiState.userName,
                                isAdminMode = uiState.isAdminMode,
                                cartItemCount = cartItems.size,
                                onSearchClick = { viewModel.setSearchOpen(true) },
                                onCartClick = { viewModel.setCartOpen(true) },
                                onAddStructureClick = { viewModel.setAddModalOpen(true) },
                                onAdminSettingsClick = { viewModel.setAdminSettingsOpen(true) },
                                onShareAppClick = { viewModel.setShareAppOpen(true) },
                                onAdminLogoutClick = { viewModel.logoutAdmin() },
                                onTriggerAdmin = { viewModel.openAdminFromSearch() }
                            )
                        }

                        // Broadcast Animated Announcement Bar
                        AnimatedVisibility(
                            visible = !uiState.broadcastMessage.isNullOrBlank(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            uiState.broadcastMessage?.let { rawMsg ->
                                val msg = rawMsg.replace("@user", uiState.userName.ifBlank { "Valued Customer" })
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = WayStockPrimary
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.sendBroadcastNotification("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        BreadcrumbBar(
                            pathStack = uiState.pathStack,
                            onBreadcrumbClick = { index -> viewModel.jumpToBreadcrumb(index) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(WayStockBg)
            ) {
                when (uiState.currentTab) {
                    "price" -> {
                        PriceManagementScreen(viewModel = viewModel)
                    }
                    "attendance" -> {
                        StaffAttendanceScreen(viewModel = viewModel)
                    }
                    "khata" -> {
                        KhataBookScreen(viewModel = viewModel)
                    }
                    else -> {
                        // Animated Folder Transition (Smooth Right-to-Left slide when opening folder, Left-to-Right when going back)
                        AnimatedContent(
                            targetState = uiState.currentFolder,
                            transitionSpec = {
                                val isDeeper = targetState.length >= initialState.length
                                if (isDeeper) {
                                    (slideInHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(280)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 2 } + fadeOut(animationSpec = tween(200)))
                                } else {
                                    (slideInHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 2 } + fadeIn(animationSpec = tween(280)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth / 2 } + fadeOut(animationSpec = tween(200)))
                                }
                            },
                            label = "FolderTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetFolder ->
                            val itemsInFolder = remember(allInventoryItems, targetFolder) {
                                allInventoryItems.filter { it.parent == targetFolder }
                            }

                            if (itemsInFolder.isEmpty()) {
                                // Empty State
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("📦✨", fontSize = 50.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Oho! Maal-Gadi Khali Hai",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WayStockDark
                                    )
                                    Text(
                                        text = "Lagta hai abhi tak koi stock nahi aaya. Chalo, kuch naya bharte hain!",
                                        fontSize = 13.sp,
                                        color = WayStockTextSec,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                                    )

                                    Button(
                                        onClick = {
                                            if (uiState.isAdminMode) {
                                                viewModel.setAddModalOpen(true)
                                            } else {
                                                viewModel.setCartOpen(true)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                                    ) {
                                        Text(
                                            text = if (uiState.isAdminMode) "Maal Bharo! 🚀" else "Bucket Dekho! 🛒",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(itemsInFolder, key = { it.key }) { item ->
                                        val cartItem = cartItems.find { it.key == item.key }
                                        val cartQty = cartItem?.quantity ?: 0
                                        val cartUnit = cartItem?.unit ?: item.currentUnit
                                        val isSelected = uiState.selectedCardKeys.contains(item.key)

                                        InventoryCard(
                                            item = item,
                                            cartQuantity = cartQty,
                                            cartUnit = cartUnit,
                                            isAdminMode = uiState.isAdminMode,
                                            isSelected = isSelected,
                                            isSelectionMode = uiState.isSelectionMode,
                                            onCardClick = {
                                                if (item.type == "folder") {
                                                    viewModel.navigateFolder(item.key)
                                                }
                                            },
                                            onCardLongClick = {
                                                viewModel.toggleCardSelection(item.key)
                                            },
                                            onAddToCart = { viewModel.addToCart(item.key) },
                                            onUpdateQuantity = { change ->
                                                viewModel.updateCartQuantity(item.key, change)
                                            },
                                            onFolderToggleChange = { isChecked ->
                                                viewModel.toggleFolderPrefix(item.key, isChecked)
                                            },
                                            onAddSubItemClick = {
                                                viewModel.openAddSubItemModal(item.key)
                                            },
                                            onUnitSelected = { newUnit ->
                                                if (cartQty > 0) {
                                                    viewModel.updateCartUnit(item.key, newUnit)
                                                }
                                            },
                                            onAddCustomUnit = { unitName ->
                                                viewModel.addCustomUnitToCategoryTree(item.key, unitName)
                                            },
                                            onDeleteCustomUnit = { unitName ->
                                                viewModel.deleteCustomUnitFromCategoryTree(item.key, unitName)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-screen Search Overlay with smooth Slide-Up Animation
        AnimatedVisibility(
            visible = uiState.isSearchOpen,
            enter = slideInVertically(animationSpec = tween(250)) { fullHeight -> fullHeight } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(220)) { fullHeight -> fullHeight } + fadeOut()
        ) {
            SearchOverlay(
                allInventoryItems = allInventoryItems,
                cartItems = cartItems,
                searchHistory = searchHistory,
                onDismiss = { viewModel.setSearchOpen(false) },
                onSuggestionClick = { key ->
                    viewModel.navigateFolder(key)
                    viewModel.setSearchOpen(false)
                },
                onAddToCart = { key -> viewModel.addToCart(key) },
                onVoiceCommand = { cmd -> viewModel.handleVoiceCommand(cmd) },
                onTriggerAdmin = { viewModel.openAdminFromSearch() }
            )
        }

        // Cart Sheet ("My Bucket") with smooth Right-to-Left Slide Animation
        AnimatedVisibility(
            visible = uiState.isCartOpen,
            enter = slideInHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth } + fadeIn(),
            exit = slideOutHorizontally(animationSpec = tween(250)) { fullWidth -> fullWidth } + fadeOut()
        ) {
            CartSheet(
                cartItems = cartItems,
                allInventoryItems = allInventoryItems,
                isAdminMode = uiState.isAdminMode,
                onDismiss = { viewModel.setCartOpen(false) },
                onUpdateQuantity = { key, change -> viewModel.updateCartQuantity(key, change) },
                onSetQuantityDirectly = { key, qty -> viewModel.setCartQuantityDirectly(key, qty) },
                onUpdateUnit = { key, unit -> viewModel.updateCartUnit(key, unit) },
                onRemoveItem = { key -> viewModel.removeFromCart(key) },
                onGeneratePreview = { viewModel.setOrderSlipOpen(true) },
                onAddCustomUnit = { key, unit -> viewModel.addCustomUnitToCategoryTree(key, unit) },
                onDeleteCustomUnit = { key, unit -> viewModel.deleteCustomUnitFromCategoryTree(key, unit) },
                onAddCustomItem = { name, cat, qty, unit -> viewModel.addCustomCartItem(name, cat, qty, unit) }
            )
        }

        // Onboarding Dialog (Shown only once for normal users when not in admin mode)
        if (!uiState.isOnboarded && !uiState.isAdminMode) {
            UserOnboardingDialog(
                onNameSubmitted = { name -> viewModel.setUserProfile(name) }
            )
        }

        // Admin Gateway PIN Modal
        if (uiState.isAdminGatewayOpen) {
            AdminGatewayDialog(
                onDismiss = { viewModel.setAdminGatewayOpen(false) },
                onValidatePin = { pin -> viewModel.validateAdminPin(pin) }
            )
        }

        // Bulk Add / Excel Structure Modal
        if (uiState.isAddModalOpen) {
            BulkAddModal(
                currentParentFolder = uiState.addModalTargetFolder ?: uiState.currentFolder,
                editTargetKey = uiState.editTargetKey,
                allExistingItems = allInventoryItems,
                initialText = uiState.addModalInitialText,
                onDismiss = { viewModel.setAddModalOpen(false) },
                onSubmitStructure = { text -> viewModel.processBulkStructure(text) }
            )
        }

        // Order Slip Preview Dialog
        if (uiState.isOrderSlipOpen) {
            OrderSlipPreviewDialog(
                cartItems = cartItems,
                inventoryItems = allInventoryItems,
                onDismiss = { viewModel.setOrderSlipOpen(false) },
                onSlipSavedOrShared = { keysToFlush ->
                    keysToFlush.forEach { key -> viewModel.removeFromCart(key) }
                    viewModel.showAlert("✅ Order Slip processed! Items cleared from Bucket.", "success")
                    if (cartItems.size <= keysToFlush.size) {
                        viewModel.setOrderSlipOpen(false)
                    }
                }
            )
        }

        // Admin Settings Dialog
        if (uiState.isAdminSettingsOpen) {
            AdminSettingsDialog(
                userRequestedItems = userRequestedItems,
                allAdminDevices = allAdminDevices,
                loggedInAdminEmail = uiState.loggedInAdminEmail,
                loggedInAdminName = uiState.loggedInAdminName,
                isSuperAdmin = uiState.isSuperAdmin,
                isAutoLaunchEnabled = uiState.isAutoLaunchEnabled,
                isGoogleAuthLoading = uiState.isGoogleAuthLoading,
                masterPinLastModifiedBy = uiState.masterPinLastModifiedBy,
                masterPinLastModifiedAt = uiState.masterPinLastModifiedAt,
                masterSecurityConfig = masterSecurityConfig,
                adminAuthManager = viewModel.adminAuthManager,
                onDismiss = { viewModel.setAdminSettingsOpen(false) },
                onGoogleLoginSuccess = { email, name -> viewModel.handleGoogleLoginResult(email, name) },
                onGoogleLoginLoading = { loading -> viewModel.setGoogleAuthLoading(loading) },
                onGoogleLogout = { viewModel.logoutAdminGoogle() },
                onToggleAutoLaunch = { isEnabled -> viewModel.toggleAutoLaunch(isEnabled) },
                onRemoteToggleDevice = { targetEmail, isEnabled -> viewModel.remoteToggleDeviceBySuperAdmin(targetEmail, isEnabled) },
                onUpdateDevicePermissions = { targetEmail, perms -> viewModel.updateDevicePermissions(targetEmail, perms) },
                onUpdateMasterGlobalConfig = { isPrice, isKhata, isStaff, isInv -> viewModel.updateMasterGlobalConfig(isPrice, isKhata, isStaff, isInv) },
                onDeleteAdminDevice = { targetEmail -> viewModel.deleteAdminDeviceBySuperAdmin(targetEmail) },
                onUpdatePassword = { oldPin, newPin -> viewModel.updateAdminPassword(oldPin, newPin) },
                onSendBroadcast = { msg -> viewModel.sendBroadcastNotification(msg) },
                onLogoutAdmin = { viewModel.logoutAdmin() },
                onDeleteRequestedItem = { id -> viewModel.deleteRequestedItem(id) },
                onClearAllRequestedItems = { viewModel.clearAllRequestedItems() },
                onAddRequestedToInventory = { structureStr -> viewModel.openAddModalWithRequestedItems(structureStr) }
            )
        }

        // Share App QR Card Dialog
        if (uiState.isShareAppOpen) {
            ShareAppCardDialog(
                onDismiss = { viewModel.setShareAppOpen(false) }
            )
        }

        // Add / Edit Staff Dialog
        if (uiState.isAddStaffDialogOpen) {
            AddEditStaffDialog(
                staffToEdit = uiState.staffToEdit,
                onDismiss = { viewModel.closeAddStaffDialog() },
                onSaveStaff = { name, role, phone, salaryType, monthlySalary, dailyWage, advanceBalance ->
                    viewModel.saveStaffMember(name, role, phone, salaryType, monthlySalary, dailyWage, advanceBalance)
                }
            )
        }

        // Full Page Staff Attendance Calendar & Management Sheet
        if (uiState.isStaffDetailOpen && uiState.selectedStaffDetail != null) {
            StaffAttendanceCalendarSheet(
                staff = uiState.selectedStaffDetail!!,
                attendanceRecords = attendanceForSelectedStaff,
                isAdminMode = uiState.isAdminMode,
                onDismiss = { viewModel.closeStaffDetail() },
                onMarkDateAttendance = { date, status, note, advanceTaken ->
                    viewModel.markAttendanceForSpecificDate(
                        staffId = uiState.selectedStaffDetail!!.id,
                        staffName = uiState.selectedStaffDetail!!.name,
                        date = date,
                        status = status,
                        note = note,
                        advanceTaken = advanceTaken
                    )
                },
                onUpdateAdvance = { newAdvance ->
                    viewModel.updateStaffAdvance(uiState.selectedStaffDetail!!.id, newAdvance)
                },
                onDeleteStaff = { staffId ->
                    viewModel.deleteStaffMember(staffId)
                }
            )
        }
    }
}
