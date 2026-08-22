package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminAuthManager
import com.example.data.AdminDeviceEntity
import com.example.data.CartItemEntity
import com.example.data.InventoryItemEntity
import com.example.data.MasterSecurityConfig
import com.example.data.SearchHistoryEntity
import com.example.data.UserRequestedItemEntity
import com.example.data.WayStockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WayStockUiState(
    val currentFolder: String = "root",
    val pathStack: List<String> = listOf("root"),
    val userName: String = "",
    val userId: String = "guest",
    val isOnboarded: Boolean = false,
    val isAdminMode: Boolean = false,
    val adminPin: String = "1234",
    val loggedInAdminEmail: String? = null,
    val loggedInAdminName: String? = null,
    val isSuperAdmin: Boolean = false,
    val isAutoLaunchEnabled: Boolean = false,
    val masterPinLastModifiedBy: String = "",
    val masterPinLastModifiedAt: Long = 0L,
    val isGoogleAuthLoading: Boolean = false,
    val broadcastMessage: String = "",
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isCartOpen: Boolean = false,
    val isAddModalOpen: Boolean = false,
    val addModalInitialText: String = "",
    val isAddingRequestedItems: Boolean = false,
    val isAdminSettingsOpen: Boolean = false,
    val isShareAppOpen: Boolean = false,
    val isAdminGatewayOpen: Boolean = false,
    val isOrderSlipOpen: Boolean = false,
    val selectedCardKeys: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val editTargetKey: String? = null,
    val addModalTargetFolder: String? = null,
    val alertMessage: String? = null,
    val alertType: String = "info",
    val currentTab: String = "inventory", // "inventory", "attendance", or "khata"
    val selectedAttendanceDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
    val isAddStaffDialogOpen: Boolean = false,
    val staffToEdit: com.example.data.StaffMemberEntity? = null,
    val selectedStaffDetail: com.example.data.StaffMemberEntity? = null,
    val isStaffDetailOpen: Boolean = false,
    val isAddKhataCustomerOpen: Boolean = false,
    val customerToEdit: com.example.data.KhataCustomerEntity? = null,
    val selectedKhataCustomer: com.example.data.KhataCustomerEntity? = null,
    val isKhataDetailOpen: Boolean = false,
    val isAddKhataTxnOpen: Boolean = false,
    val khataTxnTypeToAdd: String = "GAVE", // "GAVE" or "GOT"
    val khataSearchQuery: String = "",
    val khataFilterType: String = "ALL", // "ALL", "CUSTOMERS", "SUPPLIERS", "PENDING"
    val isKhataStickyNotificationEnabled: Boolean = false,
    val isStickyBottomMemoBarEnabled: Boolean = true,
    val isKhataMemosOpen: Boolean = false
)

class WayStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WayStockRepository(application)
    val adminAuthManager = AdminAuthManager(application)
    val khataCloudManager = com.example.data.KhataCloudManager(application, repository)
    val stockCloudManager = com.example.data.StockCloudManager(application, repository)

    private val _uiState = MutableStateFlow(WayStockUiState())
    val uiState: StateFlow<WayStockUiState> = _uiState.asStateFlow()

    val allAdminDevices: StateFlow<List<AdminDeviceEntity>> = adminAuthManager.getAllAdminDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val masterSecurityConfig: StateFlow<MasterSecurityConfig> = adminAuthManager.getMasterSecurityConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MasterSecurityConfig())

    val allInventoryItems: StateFlow<List<InventoryItemEntity>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPriceHistory: StateFlow<List<com.example.data.PriceHistoryEntity>> = repository.allPriceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemEntity>> = _uiState.flatMapLatest { state ->
        repository.getCartItems(state.userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = _uiState.flatMapLatest { state ->
        repository.getSearchHistory(state.userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRequestedItems: StateFlow<List<UserRequestedItemEntity>> = repository.allRequestedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStaffMembers: StateFlow<List<com.example.data.StaffMemberEntity>> = repository.allStaffMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceForSelectedDate: StateFlow<List<com.example.data.AttendanceRecordEntity>> = _uiState.flatMapLatest { state ->
        repository.getAttendanceForDate(state.selectedAttendanceDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceForCurrentMonth: StateFlow<List<com.example.data.AttendanceRecordEntity>> = _uiState.flatMapLatest { state ->
        val monthPrefix = state.selectedAttendanceDate.substring(0, 7) // "YYYY-MM"
        repository.getAttendanceForMonth(monthPrefix)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceForSelectedStaff: StateFlow<List<com.example.data.AttendanceRecordEntity>> = _uiState.flatMapLatest { state ->
        val staff = state.selectedStaffDetail
        if (staff != null && staff.id.isNotBlank()) {
            repository.getAttendanceForStaff(staff.id, staff.name)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKhataCustomers: StateFlow<List<com.example.data.KhataCustomerEntity>> = repository.allKhataCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKhataTransactions: StateFlow<List<com.example.data.KhataTransactionEntity>> = repository.allKhataTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCustomerTransactions: StateFlow<List<com.example.data.KhataTransactionEntity>> = _uiState.flatMapLatest { state ->
        val cust = state.selectedKhataCustomer
        if (cust != null && cust.id.isNotBlank()) {
            repository.getTransactionsForCustomer(cust.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKhataMemos: StateFlow<List<com.example.data.KhataMemoEntity>> = repository.allKhataMemos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.seedDefaultStaffIfEmpty()
            repository.seedDefaultKhataIfEmpty()
            syncFolderAndItemTypes()
            khataCloudManager.startRealtimeSync()
            
            stockCloudManager.setOnInventoryUpdatedCallback {
                viewModelScope.launch {
                    syncFolderAndItemTypes()
                }
            }
            stockCloudManager.startRealtimeSync()
        }

        // Listen for Global Broadcasts from Cloud
        viewModelScope.launch {
            stockCloudManager.latestBroadcast.collect { pair ->
                if (pair != null) {
                    val (msg, _) = pair
                    val clientName = _uiState.value.userName.ifBlank { "Customer" }
                    val personalized = msg.replace("@user", clientName, ignoreCase = true)
                    _uiState.update { it.copy(broadcastMessage = personalized) }
                }
            }
        }

        // Listen for Cloud Admin Password updates
        viewModelScope.launch {
            stockCloudManager.cloudAdminPassword.collect { pass ->
                if (!pass.isNullOrBlank()) {
                    _uiState.update { it.copy(adminPin = pass) }
                }
            }
        }

        // Restore local user profile if already onboarded
        val isUserOnboarded = adminAuthManager.isLocalUserOnboarded()
        if (isUserOnboarded) {
            val (savedUserName, savedUserId) = adminAuthManager.getLocalUserProfile()
            _uiState.update {
                it.copy(
                    userName = savedUserName,
                    userId = savedUserId,
                    isOnboarded = true
                )
            }
        }

        // Check local auto-launch preference immediately on startup
        val isLocallyEnabled = adminAuthManager.isLocalAutoLaunchEnabled()
        if (isLocallyEnabled) {
            val (savedEmail, savedName) = adminAuthManager.getLocalAdminProfile()
            _uiState.update {
                it.copy(
                    isAdminMode = true,
                    isAutoLaunchEnabled = true,
                    isOnboarded = true,
                    loggedInAdminEmail = savedEmail,
                    loggedInAdminName = savedName ?: "Administrator",
                    isSuperAdmin = if (savedEmail != null) adminAuthManager.isSuperAdmin(savedEmail) else true
                )
            }
        }

        // Restore sticky khata notification state and bottom memo bar state
        val isStickyOn = com.example.service.KhataStickyNotificationService.isStickyKhataEnabled(application)
        val isBottomMemoOn = adminAuthManager.isStickyBottomMemoBarEnabled()
        _uiState.update { 
            it.copy(
                isKhataStickyNotificationEnabled = isStickyOn,
                isStickyBottomMemoBarEnabled = isBottomMemoOn
            ) 
        }

        // Listen for Firestore Master Security PIN & update local state
        viewModelScope.launch {
            masterSecurityConfig.collect { config ->
                _uiState.update {
                    it.copy(
                        adminPin = config.masterPin,
                        masterPinLastModifiedBy = config.lastModifiedBy,
                        masterPinLastModifiedAt = config.lastModifiedAt
                    )
                }
            }
        }

        // Check if an authenticated user has auto-launch enabled online
        viewModelScope.launch {
            try {
                val currentFbUser = adminAuthManager.currentFirebaseUser
                if (currentFbUser != null && currentFbUser.email != null) {
                    val email = currentFbUser.email!!
                    val isEligible = adminAuthManager.checkAutoLaunchEligibility(email)
                    if (isEligible || isLocallyEnabled) {
                        _uiState.update {
                            it.copy(
                                isAdminMode = true,
                                loggedInAdminEmail = email,
                                loggedInAdminName = currentFbUser.displayName ?: "Administrator",
                                isSuperAdmin = adminAuthManager.isSuperAdmin(email),
                                isAutoLaunchEnabled = true
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun handleGoogleLoginResult(email: String, displayName: String) {
        val isSuper = adminAuthManager.isSuperAdmin(email)
        _uiState.update {
            it.copy(
                loggedInAdminEmail = email,
                loggedInAdminName = displayName,
                isSuperAdmin = isSuper,
                isGoogleAuthLoading = false
            )
        }
        showAlert(
            if (isSuper) "👑 Welcome Super Administrator!" else "✅ Logged in as ${displayName}",
            "success"
        )
    }

    fun signInUserWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            setGoogleAuthLoading(true)
            val result = adminAuthManager.signInWithGoogle(activityContext)
            setGoogleAuthLoading(false)
            if (result.isSuccess) {
                val (email, displayName) = result.getOrNull() ?: Pair("", "")
                val isSuper = adminAuthManager.isSuperAdmin(email)
                if (isSuper) {
                    _uiState.update {
                        it.copy(
                            userName = displayName,
                            userId = "USER_${email.replace("@", "_").replace(".", "_")}",
                            isOnboarded = true,
                            isAdminMode = true,
                            loggedInAdminEmail = email,
                            loggedInAdminName = displayName,
                            isSuperAdmin = true
                        )
                    }
                    adminAuthManager.setLocalUserProfile(displayName, "USER_${email.replace("@", "_").replace(".", "_")}")
                    showAlert("👑 Welcome Super Administrator ($displayName)!", "success")
                } else {
                    val uid = "USER_${email.replace("@", "_").replace(".", "_")}"
                    adminAuthManager.setLocalUserProfile(displayName, uid)
                    _uiState.update {
                        it.copy(
                            userName = displayName,
                            userId = uid,
                            isOnboarded = true,
                            loggedInAdminEmail = email,
                            loggedInAdminName = displayName
                        )
                    }
                    showAlert("Welcome, $displayName! 🚀", "success")
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Google Sign-In failed"
                showAlert(err, "error")
            }
        }
    }

    fun setGoogleAuthLoading(loading: Boolean) {
        _uiState.update { it.copy(isGoogleAuthLoading = loading) }
    }

    fun logoutAdminGoogle() {
        viewModelScope.launch {
            adminAuthManager.signOut()
            _uiState.update {
                it.copy(
                    loggedInAdminEmail = null,
                    loggedInAdminName = null,
                    isSuperAdmin = false,
                    isAutoLaunchEnabled = false
                )
            }
            showAlert("Logged out from Admin Account", "info")
        }
    }

    fun toggleAutoLaunch(isEnabled: Boolean) {
        val state = _uiState.value
        val email = state.loggedInAdminEmail ?: "admin@waystock.internal"
        val name = state.loggedInAdminName ?: "Administrator"

        // Always persist locally on device immediately so it works even in offline / emulator
        adminAuthManager.setLocalAutoLaunchEnabled(isEnabled, email, name)
        _uiState.update { it.copy(isAutoLaunchEnabled = isEnabled) }

        if (isEnabled) {
            showAlert("🚀 Auto-Launch ON! Admin portal will open directly on app startup.", "success")
        } else {
            showAlert("Auto-Launch disabled.", "info")
        }

        // Sync with cloud in background if email is present
        viewModelScope.launch {
            try {
                adminAuthManager.setAutoLaunchForUser(
                    email = email,
                    displayName = name,
                    isEnabled = isEnabled
                )
            } catch (_: Exception) {}
        }
    }

    fun remoteToggleDeviceBySuperAdmin(targetEmail: String, isEnabled: Boolean) {
        if (!_uiState.value.isSuperAdmin) {
            showAlert("🚫 Only Super Admin can remote-toggle devices!", "error")
            return
        }

        viewModelScope.launch {
            val res = adminAuthManager.setDeviceEnabledBySuperAdmin(targetEmail, isEnabled)
            if (res.isSuccess) {
                showAlert("Device ${if (isEnabled) "Enabled ✅" else "Disabled ⛔"}", "success")
            } else {
                showAlert("Failed to update device: ${res.exceptionOrNull()?.message}", "error")
            }
        }
    }

    fun deleteAdminDeviceBySuperAdmin(targetEmail: String) {
        if (!_uiState.value.isSuperAdmin) {
            showAlert("🚫 Only Super Admin can remove devices!", "error")
            return
        }

        viewModelScope.launch {
            val res = adminAuthManager.deleteAdminDevice(targetEmail)
            if (res.isSuccess) {
                showAlert("🗑️ Removed device from Admin list", "info")
            } else {
                showAlert("Failed to remove: ${res.exceptionOrNull()?.message}", "error")
            }
        }
    }

    suspend fun syncFolderAndItemTypes() {
        val all = repository.allInventoryItems.first()
        if (all.isEmpty()) return
        val parentKeysWithChildren = all.map { it.parent }.toSet()
        all.forEach { item ->
            val shouldBeFolder = parentKeysWithChildren.contains(item.key)
            val targetType = if (shouldBeFolder) "folder" else "item"
            if (item.type != targetType) {
                repository.insertOrUpdateItem(item.copy(type = targetType))
            }
        }
    }

    fun showAlert(message: String, type: String = "info") {
        _uiState.update { it.copy(alertMessage = message, alertType = type) }
    }

    fun clearAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    fun setUserProfile(name: String) {
        val cleanName = name.trim().split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        val uid = "USER_${System.currentTimeMillis()}"
        adminAuthManager.setLocalUserProfile(cleanName, uid)
        _uiState.update {
            it.copy(userName = cleanName, userId = uid, isOnboarded = true)
        }
        showAlert("Welcome, $cleanName! 🚀", "success")
    }

    fun navigateFolder(key: String, queryName: String? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val item = repository.getItemByKey(key)
            val displayName = queryName ?: item?.displayName ?: item?.name ?: key.substringAfterLast(">")
            
            // Record to search history for quick access
            repository.addSearchHistory(
                SearchHistoryEntity(
                    itemKey = key,
                    queryText = displayName,
                    timestamp = System.currentTimeMillis(),
                    userId = state.userId
                )
            )
        }

        _uiState.update { state ->
            val newStack = if (state.pathStack.contains(key)) {
                val index = state.pathStack.indexOf(key)
                state.pathStack.subList(0, index + 1)
            } else {
                state.pathStack + key
            }
            state.copy(currentFolder = key, pathStack = newStack)
        }
    }

    fun jumpToBreadcrumb(index: Int) {
        _uiState.update { state ->
            if (index >= 0 && index < state.pathStack.size) {
                val newStack = state.pathStack.subList(0, index + 1)
                val targetKey = newStack.last()
                state.copy(currentFolder = targetKey, pathStack = newStack)
            } else state
        }
    }

    fun navigateBack() {
        val state = _uiState.value
        if (state.pathStack.size > 1) {
            jumpToBreadcrumb(state.pathStack.size - 2)
        }
    }

    fun toggleCardSelection(key: String) {
        _uiState.update { state ->
            val newSet = state.selectedCardKeys.toMutableSet()
            if (newSet.contains(key)) {
                newSet.remove(key)
            } else {
                newSet.add(key)
            }
            val selectionActive = newSet.isNotEmpty()
            state.copy(selectedCardKeys = newSet, isSelectionMode = selectionActive)
        }
    }

    fun selectAllInCurrentFolder(currentItems: List<InventoryItemEntity>) {
        val keys = currentItems.map { it.key }.toSet()
        _uiState.update { it.copy(selectedCardKeys = keys, isSelectionMode = true) }
    }

    fun clearCardSelection() {
        _uiState.update { it.copy(selectedCardKeys = emptySet(), isSelectionMode = false, editTargetKey = null) }
    }

    fun addToCart(itemKey: String) {
        viewModelScope.launch {
            val item = repository.getItemByKey(itemKey) ?: return@launch
            val state = _uiState.value
            val rootFolder = if (itemKey.contains(">")) itemKey.split(">").first().trim() else "Home"
            val defaultUnit = item.allowedUnitsCsv.split(",").firstOrNull()?.trim() ?: "Box"

            val cartEntity = CartItemEntity(
                key = item.key,
                name = item.name,
                fullPath = item.key,
                rootFolder = rootFolder,
                quantity = 1,
                unit = defaultUnit,
                userId = state.userId
            )
            repository.insertOrUpdateCartItem(cartEntity)
            showAlert("✅ ${item.name} added to Bucket!", "success")
        }
    }

    fun addCustomCartItem(name: String, category: String, quantity: Int, unit: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val cleanName = name.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            val cleanCategory = category.trim().ifBlank { "General" }
            val cleanUnit = unit.trim().ifBlank { "Box" }
            val customKey = "custom_${cleanCategory}_${System.currentTimeMillis()}"

            // 1. Add to user's cart (temporary item, categorized under selected root folder)
            val cartEntity = CartItemEntity(
                key = customKey,
                name = cleanName,
                fullPath = "$cleanCategory>$cleanName",
                rootFolder = cleanCategory,
                quantity = if (quantity > 0) quantity else 1,
                unit = cleanUnit,
                userId = state.userId
            )
            repository.insertOrUpdateCartItem(cartEntity)

            // 2. Add to user requested items pool for Admin
            val requester = if (state.userName.isNotBlank()) state.userName else "User"
            repository.insertRequestedItem(
                UserRequestedItemEntity(
                    name = cleanName,
                    category = cleanCategory,
                    unit = cleanUnit,
                    requestedBy = requester
                )
            )

            showAlert("✅ '$cleanName' added to Bucket under $cleanCategory!", "success")
        }
    }

    fun updateCartQuantity(itemKey: String, change: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val current = cartItems.value.find { it.key == itemKey } ?: return@launch
            val newQty = current.quantity + change
            if (newQty <= 0) {
                repository.deleteCartItem(itemKey, state.userId)
                showAlert("Item removed from Bucket", "info")
            } else {
                repository.insertOrUpdateCartItem(current.copy(quantity = newQty))
            }
        }
    }

    fun setCartQuantityDirectly(itemKey: String, newQty: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val current = cartItems.value.find { it.key == itemKey } ?: return@launch
            if (newQty <= 0) {
                repository.deleteCartItem(itemKey, state.userId)
            } else {
                repository.insertOrUpdateCartItem(current.copy(quantity = newQty))
            }
        }
    }

    fun updateCartUnit(itemKey: String, newUnit: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val current = cartItems.value.find { it.key == itemKey } ?: return@launch
            repository.insertOrUpdateCartItem(current.copy(unit = newUnit))
        }
    }

    fun removeFromCart(itemKey: String) {
        viewModelScope.launch {
            val state = _uiState.value
            repository.deleteCartItem(itemKey, state.userId)
            showAlert("Item removed from Bucket", "info")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.clearCart(state.userId)
            showAlert("Bucket cleared!", "info")
        }
    }

    fun toggleFolderPrefix(itemKey: String, isChecked: Boolean) {
        viewModelScope.launch {
            val existing = repository.getItemByKey(itemKey) ?: return@launch
            repository.insertOrUpdateItem(existing.copy(toggleOn = isChecked))
            stockCloudManager.scheduleSyncToFirebase()
        }
    }

    fun processBulkStructure(rawData: String) {
        viewModelScope.launch {
            val text = rawData.trim()
            if (text.isEmpty()) return@launch

            val state = _uiState.value
            if (state.editTargetKey != null) {
                // Edit mode
                val targetKey = state.editTargetKey
                val item = repository.getItemByKey(targetKey)
                if (item != null) {
                    val formattedName = text.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    repository.insertOrUpdateItem(item.copy(name = formattedName, displayName = formattedName))
                    stockCloudManager.scheduleSyncToFirebase()
                    showAlert("Name updated & synced to Cloud! ✅", "success")
                }
                _uiState.update { it.copy(isAddModalOpen = false, editTargetKey = null, addModalTargetFolder = null) }
                clearCardSelection()
                return@launch
            }

            // Target folder to insert into (either explicitly clicked item/folder or current folder)
            val baseParent = state.addModalTargetFolder ?: state.currentFolder

            // Multi-level hierarchy parsing with support for comma-separated items at each level
            // e.g. "vegetable > fruit > mango, banana, grapes, orange > green"
            val lines = text.split("\n", "\t").map { it.trim() }.filter { it.isNotEmpty() }
            lines.forEach { line ->
                val levelSegments = line.split(">").map { it.trim() }.filter { it.isNotEmpty() }
                if (levelSegments.isEmpty()) return@forEach

                // For each level segment, split by ',' to get multiple sibling items/folders
                val levelItemLists: List<List<String>> = levelSegments.map { segment ->
                    segment.split(",")
                        .map { itemName ->
                            itemName.trim().split(" ")
                                .filter { it.isNotBlank() }
                                .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
                        }
                        .filter { it.isNotBlank() }
                }.filter { it.isNotEmpty() }

                if (levelItemLists.isEmpty()) return@forEach

                var currentParents = listOf(baseParent)

                levelItemLists.forEachIndexed { lvlIndex, namesAtThisLevel ->
                    val isLast = (lvlIndex == levelItemLists.size - 1)
                    val nextParents = mutableListOf<String>()

                    currentParents.forEach { parentKey ->
                        namesAtThisLevel.forEach { name ->
                            val uniqueKey = if (parentKey == "root") name else "$parentKey>$name"
                            val existing = repository.getItemByKey(uniqueKey)

                            if (existing == null) {
                                val newItem = InventoryItemEntity(
                                    key = uniqueKey,
                                    name = name,
                                    displayName = name,
                                    type = if (isLast) "item" else "folder",
                                    parent = parentKey
                                )
                                repository.insertOrUpdateItem(newItem)
                            } else if (!isLast && existing.type != "folder") {
                                // If intermediate node already existed as an item, promote it to folder
                                repository.insertOrUpdateItem(existing.copy(type = "folder"))
                            }

                            nextParents.add(uniqueKey)
                        }
                    }
                    currentParents = nextParents
                }
            }
            syncFolderAndItemTypes()
            stockCloudManager.scheduleSyncToFirebase()

            // If this structure was built from User Requested Items, auto-clear the requested pool
            if (state.isAddingRequestedItems) {
                repository.clearAllRequestedItems()
            }

            showAlert("Structure updated & syncing to Cloud! 🚀", "success")
            _uiState.update { it.copy(isAddModalOpen = false, addModalTargetFolder = null, addModalInitialText = "", isAddingRequestedItems = false) }
        }
    }

    fun bulkAddToCartSelected() {
        viewModelScope.launch {
            val keys = _uiState.value.selectedCardKeys
            val items = allInventoryItems.value.filter { keys.contains(it.key) && it.type == "item" }
            var count = 0
            val userId = _uiState.value.userId

            items.forEach { item ->
                val rootFolder = if (item.key.contains(">")) item.key.split(">").first().trim() else "Home"
                val defaultUnit = item.allowedUnitsCsv.split(",").firstOrNull()?.trim() ?: "Box"
                repository.insertOrUpdateCartItem(
                    CartItemEntity(
                        key = item.key,
                        name = item.name,
                        fullPath = item.key,
                        rootFolder = rootFolder,
                        quantity = 1,
                        unit = defaultUnit,
                        userId = userId
                    )
                )
                count++
            }
            showAlert("✅ $count items added to Bucket!", "success")
            clearCardSelection()
        }
    }

    fun bulkDeleteSelected() {
        viewModelScope.launch {
            val keys = _uiState.value.selectedCardKeys
            val userId = _uiState.value.userId
            keys.forEach { key ->
                repository.deleteItemAndChildren(key)
                repository.deleteCartItemsByKeys(key, rootFolder = key, userId = userId)
            }
            syncFolderAndItemTypes()
            stockCloudManager.scheduleSyncToFirebase()
            showAlert("Selected items deleted & synced to Cloud! 🗑️", "success")
            clearCardSelection()
        }
    }

    fun addCustomUnitToCategoryTree(categoryKey: String, unitName: String) {
        viewModelScope.launch {
            val formatted = unitName.trim().replaceFirstChar { it.uppercase() }
            if (formatted.isEmpty()) return@launch

            val rootParent = if (categoryKey.contains(">")) categoryKey.split(">").first().trim() else categoryKey
            val all = allInventoryItems.value

            all.forEach { item ->
                if (item.key == rootParent || item.key.startsWith("$rootParent>")) {
                    val units = item.allowedUnitsCsv.split(",").map { it.trim() }.toMutableList()
                    if (!units.contains(formatted)) {
                        units.add(formatted)
                        val newCsv = units.joinToString(",")
                        repository.insertOrUpdateItem(item.copy(allowedUnitsCsv = newCsv, currentUnit = formatted))
                    }
                }
            }
            stockCloudManager.scheduleSyncToFirebase()
            showAlert("Unit '$formatted' added & synced to Cloud! ✅", "success")
        }
    }

    fun deleteCustomUnitFromCategoryTree(categoryKey: String, unitToDelete: String) {
        viewModelScope.launch {
            val rootParent = if (categoryKey.contains(">")) categoryKey.split(">").first().trim() else categoryKey
            val all = allInventoryItems.value

            all.forEach { item ->
                if (item.key == rootParent || item.key.startsWith("$rootParent>")) {
                    val units = item.allowedUnitsCsv.split(",").map { it.trim() }.filter { it != unitToDelete }
                    val newCsv = if (units.isEmpty()) "Box" else units.joinToString(",")
                    val newUnit = if (units.isEmpty()) "Box" else units.first()
                    repository.insertOrUpdateItem(item.copy(allowedUnitsCsv = newCsv, currentUnit = newUnit))
                }
            }
            stockCloudManager.scheduleSyncToFirebase()
            showAlert("Unit '$unitToDelete' deleted 🗑️", "info")
        }
    }

    fun syncWithCloudNow() {
        viewModelScope.launch {
            showAlert("Syncing inventory with Cloud... ⏳", "info")
            stockCloudManager.pullFullInventoryFromCloud()
            showAlert("Cloud inventory sync complete! ✅", "success")
        }
    }

    fun pushAllInventoryToCloudNow() {
        viewModelScope.launch {
            showAlert("Pushing all inventory to Cloud... ⏳", "info")
            val success = stockCloudManager.syncToFirebaseImmediate()
            if (success) {
                showAlert("All inventory pushed to Cloud successfully! ☁️", "success")
            } else {
                showAlert("Failed to push inventory to Cloud", "error")
            }
        }
    }

    fun validateAdminPin(pin: String): Boolean {
        return if (pin == _uiState.value.adminPin) {
            _uiState.update { it.copy(isAdminMode = true, isAdminGatewayOpen = false) }
            showAlert("Access Granted! Admin Portal Active. 🚀", "success")
            true
        } else {
            showAlert("🚫 INVALID PIN! Access Denied.", "error")
            false
        }
    }

    fun updateAdminPassword(oldPin: String, newPin: String) {
        val state = _uiState.value
        val loggedInEmail = state.loggedInAdminEmail

        // 1. Check if logged in with Google
        if (loggedInEmail.isNullOrBlank()) {
            showAlert("⚠️ Please sign in with Google first to change PIN!", "error")
            return
        }

        // 2. Check if Super Admin
        if (!state.isSuperAdmin) {
            showAlert("🚫 Access Denied! Only Master Admin can change master PIN.", "error")
            return
        }

        // 3. Verify old PIN
        if (oldPin != state.adminPin) {
            showAlert("🚫 Incorrect old PIN!", "error")
            return
        }

        // 4. Verify length
        if (newPin.length < 4) {
            showAlert("⚠️ New PIN must be at least 4 digits!", "error")
            return
        }

        viewModelScope.launch {
            val result = adminAuthManager.updateMasterPin(loggedInEmail, newPin)
            val cloudRes = stockCloudManager.updateCloudAdminPassword(newPin)
            if (result.isSuccess || cloudRes.isSuccess) {
                _uiState.update { it.copy(adminPin = newPin, isAdminSettingsOpen = false) }
                showAlert("🎉 Master PIN updated to '$newPin' & saved to cloud!", "success")
            } else {
                showAlert("⚠️ Failed to update PIN online: ${result.exceptionOrNull()?.message}", "error")
            }
        }
    }

    fun sendBroadcastNotification(message: String) {
        val msg = message.trim()
        if (msg.isEmpty()) {
            showAlert("Message cannot be empty!", "error")
            return
        }
        viewModelScope.launch {
            val cloudRes = stockCloudManager.sendGlobalBroadcast(msg)
            _uiState.update { it.copy(broadcastMessage = msg, isAdminSettingsOpen = false) }
            if (cloudRes.isSuccess) {
                showAlert("Broadcast message sent to Cloud & all users! 📢", "success")
            } else {
                showAlert("Broadcast saved locally: $msg", "info")
            }
        }
    }

    fun logoutAdmin() {
        _uiState.update { it.copy(isAdminMode = false, isAdminSettingsOpen = false) }
        showAlert("Logged out from Admin mode 🔒", "info")
    }

    fun setAddModalOpen(open: Boolean, editKey: String? = null, targetFolder: String? = null) {
        _uiState.update {
            it.copy(
                isAddModalOpen = open,
                editTargetKey = editKey,
                addModalTargetFolder = targetFolder,
                addModalInitialText = if (open) it.addModalInitialText else "",
                isAddingRequestedItems = if (open) it.isAddingRequestedItems else false
            )
        }
    }

    fun openAddSubItemModal(targetParentKey: String) {
        _uiState.update {
            it.copy(
                isAddModalOpen = true,
                addModalTargetFolder = targetParentKey,
                editTargetKey = null,
                addModalInitialText = "",
                isAddingRequestedItems = false
            )
        }
    }

    fun openAddModalWithRequestedItems(structureString: String) {
        _uiState.update {
            it.copy(
                isAdminSettingsOpen = false,
                isAddModalOpen = true,
                addModalInitialText = structureString,
                addModalTargetFolder = "root",
                editTargetKey = null,
                isAddingRequestedItems = true
            )
        }
    }

    fun deleteRequestedItem(id: Long) {
        viewModelScope.launch {
            repository.deleteRequestedItem(id)
            showAlert("Requested item removed 🗑️", "info")
        }
    }

    fun clearAllRequestedItems() {
        viewModelScope.launch {
            repository.clearAllRequestedItems()
            showAlert("Requested items list cleared! 🗑️", "info")
        }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open) }
    }

    fun setCartOpen(open: Boolean) {
        _uiState.update { it.copy(isCartOpen = open) }
    }

    fun setAdminSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isAdminSettingsOpen = open) }
    }

    fun setShareAppOpen(open: Boolean) {
        _uiState.update { it.copy(isShareAppOpen = open) }
    }

    fun setAdminGatewayOpen(open: Boolean) {
        _uiState.update { it.copy(isAdminGatewayOpen = open) }
    }

    fun setOrderSlipOpen(open: Boolean) {
        _uiState.update { it.copy(isOrderSlipOpen = open) }
    }

    fun openAdminFromSearch() {
        _uiState.update { it.copy(isSearchOpen = false, isAdminGatewayOpen = true) }
    }

    fun handleVoiceCommand(rawTranscript: String) {
        val transcript = rawTranscript.trim()
        if (transcript.isBlank()) return
        val lower = transcript.lowercase()
        val state = _uiState.value

        // Secret Admin Portal Trigger
        if (lower == "admin.html" || lower == "admin" || lower == "open admin" || lower == "admin mode" || lower == "admin portal") {
            setSearchOpen(false)
            setAdminGatewayOpen(true)
            return
        }

        // 1. Quick Bucket Actions: View / Open / Show
        val isShowBucket = lower.contains("show bucket") || lower.contains("open bucket") || lower.contains("open cart") ||
                lower.contains("view bucket") || lower.contains("view cart") || lower.contains("show cart") ||
                lower.contains("bucket dikhao") || lower.contains("cart dikhao") || lower.contains("cart kholo") ||
                lower.contains("bucket kholo") || lower.contains("bucket open") || lower.contains("cart open") ||
                lower.contains("mera bucket") || lower == "cart" || lower == "bucket" || lower == "my bucket"

        if (isShowBucket) {
            setSearchOpen(false)
            setCartOpen(true)
            showAlert("🛒 Opening your Bucket!", "info")
            return
        }

        // 2. Quick Bucket Actions: Clear / Empty
        val isClearBucket = lower.contains("clear bucket") || lower.contains("clear cart") || lower.contains("empty cart") ||
                lower.contains("empty bucket") || lower.contains("cart khali") || lower.contains("bucket khali") ||
                lower.contains("sab item remove") || lower.contains("remove all") || lower.contains("bucket saaf") ||
                lower.contains("cart saaf")

        if (isClearBucket) {
            clearCart()
            setSearchOpen(false)
            showAlert("🗑️ Bucket cleared successfully!", "info")
            return
        }

        // 3. Quick Bucket Actions: Count / Status
        val isCartStatus = lower.contains("how many") || lower.contains("kitne item") || lower.contains("cart status") ||
                lower.contains("bucket status") || lower.contains("count in cart") || lower.contains("total items")
        if (isCartStatus) {
            val totalItems = cartItems.value.size
            val totalQty = cartItems.value.sumOf { it.quantity }
            setSearchOpen(false)
            showAlert("📦 Bucket has $totalItems items ($totalQty total quantity)", "info")
            return
        }

        // 4. PDF / Order Slip / Bill / WhatsApp Command
        val isOrderSlip = lower.contains("slip") || lower.contains("pdf") || lower.contains("bill") ||
                lower.contains("whatsapp") || lower.contains("order summary") || lower.contains("parchi") ||
                lower.contains("invoice")

        if (isOrderSlip) {
            setSearchOpen(false)
            if (cartItems.value.isEmpty()) {
                showAlert("⚠️ Bucket is empty! Add items first.", "warning")
            } else {
                setOrderSlipOpen(true)
                showAlert("📄 Generating Order Slip...", "success")
            }
            return
        }

        // 5. Navigation: Back / Home / Root
        if (lower.contains("go back") || lower.contains("back jao") || lower.contains("piche jao") || lower == "back" || lower == "piche") {
            navigateBack()
            setSearchOpen(false)
            showAlert("⬅️ Navigated Back", "info")
            return
        }

        if (lower == "home" || lower == "root" || lower.contains("home jao") || lower.contains("root folder") || lower.contains("go home")) {
            _uiState.update { it.copy(pathStack = listOf("root"), currentFolder = "root") }
            setSearchOpen(false)
            showAlert("🏠 Navigated to Home", "info")
            return
        }

        // 6. Navigation: Open Folder (English / Hindi / Hinglish)
        val isNavFolder = lower.startsWith("open ") || lower.startsWith("go to ") || lower.startsWith("navigate to ") ||
                lower.startsWith("explore ") || lower.endsWith(" kholo") || lower.endsWith(" me jao") ||
                lower.endsWith(" open karo") || lower.endsWith(" dikhao") || lower.contains("folder")

        var folderTargetName = ""
        if (isNavFolder) {
            folderTargetName = lower
                .replace("open ", "")
                .replace("go to ", "")
                .replace("navigate to ", "")
                .replace("explore ", "")
                .replace("folder ", "")
                .replace(" folder", "")
                .replace(" kholo", "")
                .replace(" me jao", "")
                .replace(" open karo", "")
                .replace(" dikhao", "")
                .trim()
        }

        if (folderTargetName.isNotBlank()) {
            val folderMatch = allInventoryItems.value.find {
                it.type == "folder" && (it.name.equals(folderTargetName, ignoreCase = true) ||
                        it.name.lowercase().contains(folderTargetName) ||
                        folderTargetName.contains(it.name.lowercase()))
            }
            if (folderMatch != null) {
                navigateFolder(folderMatch.key)
                setSearchOpen(false)
                showAlert("📁 Opening folder: ${folderMatch.name}", "success")
                return
            }
        }

        // 7. Stock Check / Availability Search
        val isStockCheck = lower.contains("stock") || lower.contains("available") || lower.contains("hai kya") ||
                lower.contains("kya ") || lower.startsWith("search ") || lower.startsWith("find ") || lower.contains("dhundo") ||
                lower.startsWith("check ")

        if (isStockCheck) {
            val searchClean = lower
                .replace("do we have", "")
                .replace("check stock for", "")
                .replace("check stock", "")
                .replace("stock check karo", "")
                .replace("stock check", "")
                .replace("ka stock check karo", "")
                .replace("ka stock batao", "")
                .replace("stock batao", "")
                .replace("available hai", "")
                .replace("is available", "")
                .replace("kya", "")
                .replace("hai", "")
                .replace("search", "")
                .replace("find", "")
                .replace("dhundo", "")
                .replace("check", "")
                .replace("?", "")
                .trim()

            if (searchClean.isNotBlank()) {
                val stockMatch = allInventoryItems.value.find {
                    it.type == "item" && (it.name.equals(searchClean, ignoreCase = true) ||
                            it.name.lowercase().contains(searchClean) ||
                            searchClean.contains(it.name.lowercase()))
                }
                if (stockMatch != null) {
                    val folderDisplay = if (stockMatch.parent != "root") stockMatch.parent else "Main Category"
                    setSearchOpen(false)
                    if (stockMatch.key.contains(">")) {
                        val parentFolderKey = stockMatch.key.substringBeforeLast(">")
                        navigateFolder(parentFolderKey)
                    }
                    showAlert("✅ In Stock: '${stockMatch.name}' (Category: $folderDisplay)", "success")
                    return
                } else {
                    showAlert("🔍 No stock found for '$searchClean'", "warning")
                    return
                }
            }
        }

        // 8. Direct Add to Bucket (Cart)
        val numberMap = mapOf(
            "ek" to 1, "one" to 1, "a" to 1, "an" to 1,
            "do" to 2, "two" to 2,
            "teen" to 3, "tin" to 3, "three" to 3,
            "chaar" to 4, "char" to 4, "four" to 4,
            "paanch" to 5, "panch" to 5, "five" to 5,
            "chhe" to 6, "che" to 6, "six" to 6,
            "saat" to 7, "sat" to 7, "seven" to 7,
            "aath" to 8, "ath" to 8, "eight" to 8,
            "nau" to 9, "no" to 9, "nine" to 9,
            "dus" to 10, "das" to 10, "ten" to 10,
            "gyarah" to 11, "eleven" to 11,
            "barah" to 12, "twelve" to 12,
            "terah" to 13, "thirteen" to 13,
            "chaudah" to 14, "fourteen" to 14,
            "pandrah" to 15, "fifteen" to 15,
            "solah" to 16, "sixteen" to 16,
            "satrah" to 17, "seventeen" to 17,
            "atharah" to 18, "eighteen" to 18,
            "unnis" to 19, "nineteen" to 19,
            "bees" to 20, "twenty" to 20,
            "pachis" to 25, "twenty five" to 25,
            "tees" to 30, "thirty" to 30,
            "chalis" to 40, "forty" to 40,
            "pachas" to 50, "fifty" to 50,
            "sau" to 100, "hundred" to 100
        )

        val unitKeywords = listOf(
            "packet", "pkt", "packets", "pkts", "box", "boxes", "piece", "pieces", "pcs", "pc",
            "carton", "cartons", "ctn", "kg", "kgs", "g", "gm", "gms", "gram", "grams",
            "bottle", "bottles", "btl", "pouch", "pouches", "dabba", "dappe", "nag", "dozen", "dz"
        )

        var detectedQty = 1
        var detectedUnit: String? = null

        val digitRegex = Regex("\\b(\\d+)\\b")
        val digitMatch = digitRegex.find(lower)
        if (digitMatch != null) {
            detectedQty = digitMatch.groupValues[1].toIntOrNull() ?: 1
        } else {
            for ((word, num) in numberMap) {
                if (Regex("\\b$word\\b").containsMatchIn(lower)) {
                    detectedQty = num
                    break
                }
            }
        }

        for (unitKey in unitKeywords) {
            if (Regex("\\b$unitKey\\b").containsMatchIn(lower)) {
                detectedUnit = when (unitKey) {
                    "packet", "packets", "pkt", "pkts", "pouch", "pouches" -> "Packet"
                    "box", "boxes", "dabba", "dappe" -> "Box"
                    "piece", "pieces", "pcs", "pc", "nag" -> "Piece"
                    "carton", "cartons", "ctn" -> "Carton"
                    "bottle", "bottles", "btl" -> "Bottle"
                    "kg", "kgs" -> "KG"
                    "dozen", "dz" -> "Dozen"
                    else -> unitKey.replaceFirstChar { it.uppercase() }
                }
                break
            }
        }

        var cleanedPhrase = lower
            .replace(Regex("\\b(add|put|insert|plus|daalo|dalo|daal do|to cart|in cart|to bucket|in bucket|cart me|bucket me|karo|banao|kar do|please|of)\\b"), " ")
            .replace(digitRegex, " ")

        for (word in numberMap.keys) {
            cleanedPhrase = cleanedPhrase.replace(Regex("\\b$word\\b"), " ")
        }
        for (unitKey in unitKeywords) {
            cleanedPhrase = cleanedPhrase.replace(Regex("\\b$unitKey\\b"), " ")
        }
        cleanedPhrase = cleanedPhrase.replace(Regex("\\s+"), " ").trim()

        if (cleanedPhrase.isNotBlank()) {
            val itemMatch = allInventoryItems.value.find { item ->
                item.type == "item" && (
                    item.name.equals(cleanedPhrase, ignoreCase = true) ||
                    item.name.lowercase().contains(cleanedPhrase) ||
                    cleanedPhrase.contains(item.name.lowercase())
                )
            } ?: allInventoryItems.value.find { it.type == "item" && (it.displayName?.lowercase()?.contains(cleanedPhrase) == true) }

            if (itemMatch != null) {
                viewModelScope.launch {
                    val rootFolder = if (itemMatch.key.contains(">")) itemMatch.key.split(">").first().trim() else "Home"
                    val finalUnit = detectedUnit ?: itemMatch.allowedUnitsCsv.split(",").firstOrNull()?.trim() ?: "Box"

                    repository.insertOrUpdateCartItem(
                        CartItemEntity(
                            key = itemMatch.key,
                            name = itemMatch.name,
                            fullPath = itemMatch.key,
                            rootFolder = rootFolder,
                            quantity = detectedQty,
                            unit = finalUnit,
                            userId = state.userId
                        )
                    )
                    setSearchOpen(false)
                    showAlert("🛒 Added $detectedQty $finalUnit of '${itemMatch.name}' to Bucket!", "success")
                }
                return
            }
        }

        // 9. Generic Folder Match Fallback
        val genericFolder = allInventoryItems.value.find {
            it.type == "folder" && (it.name.equals(cleanedPhrase, ignoreCase = true) || it.name.lowercase().contains(cleanedPhrase))
        }
        if (genericFolder != null) {
            navigateFolder(genericFolder.key)
            setSearchOpen(false)
            showAlert("📁 Opening folder: ${genericFolder.name}", "success")
            return
        }

        // Generic Info Notice
        showAlert("🎙️ Heard: \"$rawTranscript\". Try 'Add 5 Lays', 'Open Snacks', 'Show Bucket', or 'Create Slip'.", "info")
    }

    // ==================== ATTENDANCE & STAFF MANAGEMENT ====================

    fun setCurrentTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSelectedAttendanceDate(date: String) {
        _uiState.update { it.copy(selectedAttendanceDate = date) }
    }

    fun markAttendance(
        staffId: String,
        staffName: String,
        status: String,
        inTime: String? = null,
        outTime: String? = null,
        overtimeHours: Double = 0.0,
        note: String = ""
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val date = state.selectedAttendanceDate
            val recordId = "${staffId}_${date}"
            val markerName = if (state.isAdminMode) (state.loggedInAdminName ?: "Admin") else (state.userName.ifBlank { "User" })

            val currentTimeFormatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            val existing = repository.getStaffAttendanceForDate(staffId, date)

            val finalInTime = inTime ?: existing?.inTime ?: if (status == "Present" || status == "Half Day") currentTimeFormatted else null
            val finalOutTime = outTime ?: existing?.outTime

            val record = com.example.data.AttendanceRecordEntity(
                id = recordId,
                staffId = staffId,
                staffName = staffName,
                date = date,
                status = status,
                inTime = finalInTime,
                outTime = finalOutTime,
                overtimeHours = overtimeHours,
                note = note,
                markedBy = markerName,
                markedAt = System.currentTimeMillis()
            )

            repository.insertOrUpdateAttendance(record)
            showAlert("✅ Marked $status for $staffName", "success")
        }
    }

    fun punchInSelf(staffMember: com.example.data.StaffMemberEntity?) {
        val state = _uiState.value
        val name = staffMember?.name ?: state.userName.ifBlank { "Staff" }
        val id = staffMember?.id ?: "STAFF_SELF_${state.userId}"
        val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

        viewModelScope.launch {
            val date = state.selectedAttendanceDate
            val existing = repository.getStaffAttendanceForDate(id, date)

            val record = com.example.data.AttendanceRecordEntity(
                id = "${id}_${date}",
                staffId = id,
                staffName = name,
                date = date,
                status = "Present",
                inTime = existing?.inTime ?: time,
                outTime = existing?.outTime,
                markedBy = state.userName.ifBlank { "Self" },
                markedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateAttendance(record)
            showAlert("🟢 Punch In successful at $time! Have a productive day.", "success")
        }
    }

    fun punchOutSelf(staffMember: com.example.data.StaffMemberEntity?) {
        val state = _uiState.value
        val name = staffMember?.name ?: state.userName.ifBlank { "Staff" }
        val id = staffMember?.id ?: "STAFF_SELF_${state.userId}"
        val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

        viewModelScope.launch {
            val date = state.selectedAttendanceDate
            val existing = repository.getStaffAttendanceForDate(id, date)

            val record = com.example.data.AttendanceRecordEntity(
                id = "${id}_${date}",
                staffId = id,
                staffName = name,
                date = date,
                status = existing?.status ?: "Present",
                inTime = existing?.inTime ?: "09:00 AM",
                outTime = time,
                markedBy = state.userName.ifBlank { "Self" },
                markedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateAttendance(record)
            showAlert("🔴 Punch Out recorded at $time! See you tomorrow.", "info")
        }
    }

    fun markAllPresentForSelectedDate() {
        viewModelScope.launch {
            val state = _uiState.value
            val date = state.selectedAttendanceDate
            val staffList = allStaffMembers.value
            val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            val markerName = if (state.isAdminMode) (state.loggedInAdminName ?: "Admin") else (state.userName.ifBlank { "User" })

            val records = staffList.map { staff ->
                com.example.data.AttendanceRecordEntity(
                    id = "${staff.id}_${date}",
                    staffId = staff.id,
                    staffName = staff.name,
                    date = date,
                    status = "Present",
                    inTime = time,
                    markedBy = markerName,
                    markedAt = System.currentTimeMillis()
                )
            }
            repository.insertAllAttendance(records)
            showAlert("✨ Marked all ${records.size} staff members as Present!", "success")
        }
    }

    fun openAddStaffDialog(staff: com.example.data.StaffMemberEntity? = null) {
        _uiState.update { it.copy(isAddStaffDialogOpen = true, staffToEdit = staff) }
    }

    fun closeAddStaffDialog() {
        _uiState.update { it.copy(isAddStaffDialogOpen = false, staffToEdit = null) }
    }

    fun saveStaffMember(
        name: String,
        role: String,
        phone: String,
        salaryType: String,
        monthlySalary: Double,
        dailyWage: Double,
        advanceBalance: Double = 0.0
    ) {
        val state = _uiState.value
        val existingId = state.staffToEdit?.id ?: "STAFF_${System.currentTimeMillis()}"

        viewModelScope.launch {
            val entity = com.example.data.StaffMemberEntity(
                id = existingId,
                name = name.trim(),
                role = role.trim().ifBlank { "Staff" },
                phone = phone.trim(),
                salaryType = salaryType,
                monthlySalary = monthlySalary,
                dailyWage = dailyWage,
                advanceBalance = advanceBalance,
                addedAt = state.staffToEdit?.addedAt ?: System.currentTimeMillis()
            )
            repository.insertOrUpdateStaffMember(entity)
            if (state.selectedStaffDetail?.id == entity.id) {
                _uiState.update { it.copy(selectedStaffDetail = entity) }
            }
            closeAddStaffDialog()
            showAlert("👥 Staff member '${entity.name}' saved!", "success")
        }
    }

    fun updateStaffAdvance(staffId: String, newAdvance: Double) {
        viewModelScope.launch {
            val all = allStaffMembers.value
            val target = all.find { it.id == staffId }
            if (target != null) {
                val updated = target.copy(advanceBalance = newAdvance)
                repository.insertOrUpdateStaffMember(updated)
                if (_uiState.value.selectedStaffDetail?.id == staffId) {
                    _uiState.update { it.copy(selectedStaffDetail = updated) }
                }
                showAlert("💰 Advance balance updated: ₹${newAdvance.toInt()}", "info")
            }
        }
    }

    fun openStaffDetail(staff: com.example.data.StaffMemberEntity) {
        _uiState.update { it.copy(selectedStaffDetail = staff, isStaffDetailOpen = true) }
    }

    fun openSelfDetail() {
        val state = _uiState.value
        val existingStaff = allStaffMembers.value.find { it.name.equals(state.userName, ignoreCase = true) }
        val selfEntity = existingStaff ?: com.example.data.StaffMemberEntity(
            id = "STAFF_SELF_${state.userId}",
            name = state.userName.ifBlank { "You (Current User)" },
            role = if (state.isAdminMode) "Admin / Owner" else "Staff Member",
            phone = "",
            salaryType = "Monthly",
            monthlySalary = 0.0,
            dailyWage = 0.0,
            advanceBalance = 0.0
        )
        _uiState.update { it.copy(selectedStaffDetail = selfEntity, isStaffDetailOpen = true) }
    }

    fun closeStaffDetail() {
        _uiState.update { it.copy(isStaffDetailOpen = false, selectedStaffDetail = null) }
    }

    fun markAttendanceForSpecificDate(
        staffId: String,
        staffName: String,
        date: String,
        status: String,
        note: String = "",
        advanceTaken: Double = 0.0
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val recordId = "${staffId}_${date}"
            val markerName = if (state.isAdminMode) (state.loggedInAdminName ?: "Admin") else (state.userName.ifBlank { "User" })
            val currentTimeFormatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

            val existing = repository.getStaffAttendanceForDate(staffId, date)
            val finalInTime = existing?.inTime ?: if (status == "Present" || status == "Half Day") currentTimeFormatted else null

            val record = com.example.data.AttendanceRecordEntity(
                id = recordId,
                staffId = staffId,
                staffName = staffName,
                date = date,
                status = status,
                inTime = finalInTime,
                outTime = existing?.outTime,
                overtimeHours = existing?.overtimeHours ?: 0.0,
                note = note.ifBlank { existing?.note ?: "" },
                advanceTaken = advanceTaken,
                markedBy = markerName,
                markedAt = System.currentTimeMillis()
            )

            repository.insertOrUpdateAttendance(record)
            showAlert("🗓️ Saved $status for $date ($staffName)", "success")
        }
    }

    fun deleteStaffMember(staffId: String) {
        viewModelScope.launch {
            repository.deleteStaffMember(staffId)
            if (_uiState.value.selectedStaffDetail?.id == staffId) {
                closeStaffDetail()
            }
            showAlert("🗑️ Staff member removed successfully.", "info")
        }
    }

    // ==========================================
    // KHATA BOOK / DIGITAL LEDGER METHODS
    // ==========================================

    fun setKhataSearchQuery(query: String) {
        _uiState.update { it.copy(khataSearchQuery = query) }
    }

    fun toggleKhataStickyNotification(enabled: Boolean) {
        com.example.service.KhataStickyNotificationService.toggleStickyNotification(getApplication(), enabled)
        _uiState.update { it.copy(isKhataStickyNotificationEnabled = enabled) }
        if (enabled) {
            showAlert("🔔 Permanent Khata Notification Bar Enabled!", "success")
        } else {
            showAlert("🔕 Permanent Khata Notification Bar Disabled", "info")
        }
    }

    fun setKhataFilterType(type: String) {
        _uiState.update { it.copy(khataFilterType = type) }
    }

    fun openAddKhataCustomer(customer: com.example.data.KhataCustomerEntity? = null) {
        _uiState.update { it.copy(isAddKhataCustomerOpen = true, customerToEdit = customer) }
    }

    fun closeAddKhataCustomer() {
        _uiState.update { it.copy(isAddKhataCustomerOpen = false, customerToEdit = null) }
    }

    fun openKhataDetail(customer: com.example.data.KhataCustomerEntity) {
        _uiState.update { it.copy(selectedKhataCustomer = customer, isKhataDetailOpen = true) }
    }

    fun closeKhataDetail() {
        _uiState.update { it.copy(selectedKhataCustomer = null, isKhataDetailOpen = false) }
    }

    fun openAddKhataTxn(type: String) {
        _uiState.update { it.copy(isAddKhataTxnOpen = true, khataTxnTypeToAdd = type) }
    }

    fun closeAddKhataTxn() {
        _uiState.update { it.copy(isAddKhataTxnOpen = false) }
    }

    fun saveKhataCustomer(
        id: String?,
        name: String,
        phone: String,
        address: String,
        customerType: String,
        initialBalance: Double = 0.0
    ) {
        viewModelScope.launch {
            val customerId = id ?: "CUST_${System.currentTimeMillis()}"
            val existing = if (id != null) repository.getKhataCustomerById(id) else null

            val customer = com.example.data.KhataCustomerEntity(
                id = customerId,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                customerType = customerType,
                balance = existing?.balance ?: initialBalance,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateKhataCustomer(customer)
            khataCloudManager.uploadCustomerToCloud(customer)
            closeAddKhataCustomer()

            // If we are editing currently open detail, update state
            if (_uiState.value.selectedKhataCustomer?.id == customerId) {
                _uiState.update { it.copy(selectedKhataCustomer = customer) }
            }

            showAlert("📒 Customer '${customer.name}' saved to Cloud!", "success")
        }
    }

    fun deleteKhataCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteKhataCustomer(customerId)
            khataCloudManager.deleteCustomerFromCloud(customerId)
            if (_uiState.value.selectedKhataCustomer?.id == customerId) {
                closeKhataDetail()
            }
            showAlert("🗑️ Customer and ledger transactions deleted from Cloud.", "info")
        }
    }

    fun addKhataTransaction(
        customerId: String,
        customerName: String,
        amount: Double,
        type: String, // "GAVE" or "GOT"
        note: String,
        paymentMode: String = "Cash",
        billNumber: String = "",
        date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    ) {
        viewModelScope.launch {
            val nowMs = System.currentTimeMillis()
            val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(nowMs))
            val cleanNote = note.trim()
            val cleanBill = billNumber.trim()

            // Smart 1-Minute Same Item Entry Auto-Multiplier Merge Logic:
            val latestTxn = repository.getLatestTransactionForCustomer(customerId)
            var merged = false

            if (latestTxn != null &&
                latestTxn.type == type &&
                (nowMs - latestTxn.timestamp) <= 60000L &&
                latestTxn.date == date
            ) {
                val extractBaseAndQty = { raw: String ->
                    val match = Regex("""^(\d+)\s*[xX×]\s*(.+)$""").find(raw.trim())
                    if (match != null) {
                        val qty = match.groupValues[1].toIntOrNull() ?: 1
                        val base = match.groupValues[2].trim()
                        Pair(base, qty)
                    } else {
                        Pair(raw.trim(), 1)
                    }
                }

                val (latestBase, latestQty) = extractBaseAndQty(latestTxn.note)
                val (newBase, newQty) = extractBaseAndQty(cleanNote)

                if (cleanNote.isNotBlank() && latestBase.equals(newBase, ignoreCase = true)) {
                    val combinedQty = latestQty + newQty
                    val updatedNote = "${combinedQty}× $latestBase"
                    val updatedAmount = latestTxn.amount + amount

                    // First delete old transaction from ledger balance & cloud
                    repository.deleteKhataTransaction(latestTxn)
                    khataCloudManager.deleteTransactionFromCloud(latestTxn.id)

                    // Insert the unified multiplied transaction
                    val mergedTxn = latestTxn.copy(
                        amount = updatedAmount,
                        note = updatedNote,
                        timestamp = nowMs,
                        time = time
                    )
                    repository.addKhataTransaction(mergedTxn)
                    khataCloudManager.uploadTransactionToCloud(mergedTxn)
                    merged = true

                    val updatedCustomer = repository.getKhataCustomerById(customerId)
                    if (updatedCustomer != null) {
                        khataCloudManager.uploadCustomerToCloud(updatedCustomer)
                        if (_uiState.value.selectedKhataCustomer?.id == customerId) {
                            _uiState.update { it.copy(selectedKhataCustomer = updatedCustomer) }
                        }
                    }

                    closeAddKhataTxn()
                    showAlert("⚡ Auto-Merged into '$updatedNote' (Total: ₹${updatedAmount.toInt()}) in Cloud", "success")
                }
            }

            if (!merged) {
                val txn = com.example.data.KhataTransactionEntity(
                    id = "TXN_$nowMs",
                    customerId = customerId,
                    customerName = customerName,
                    amount = amount,
                    type = type,
                    date = date,
                    time = time,
                    note = cleanNote,
                    paymentMode = paymentMode,
                    billNumber = cleanBill,
                    timestamp = nowMs
                )
                repository.addKhataTransaction(txn)
                khataCloudManager.uploadTransactionToCloud(txn)

                // Refresh selected customer state and sync updated balance to cloud
                val updated = repository.getKhataCustomerById(customerId)
                if (updated != null) {
                    khataCloudManager.uploadCustomerToCloud(updated)
                    if (_uiState.value.selectedKhataCustomer?.id == customerId) {
                        _uiState.update { it.copy(selectedKhataCustomer = updated) }
                    }
                }

                closeAddKhataTxn()
                val actionLabel = if (type == "GAVE") "Udhar (Diya ₹$amount)" else "Jama (Mila ₹$amount)"
                showAlert("✅ $actionLabel synced to Cloud for $customerName", "success")
            }
        }
    }

    fun deleteKhataTransaction(txn: com.example.data.KhataTransactionEntity) {
        viewModelScope.launch {
            repository.deleteKhataTransaction(txn)
            khataCloudManager.deleteTransactionFromCloud(txn.id)
            val updated = repository.getKhataCustomerById(txn.customerId)
            if (updated != null) {
                khataCloudManager.uploadCustomerToCloud(updated)
                if (_uiState.value.selectedKhataCustomer?.id == txn.customerId) {
                    _uiState.update { it.copy(selectedKhataCustomer = updated) }
                }
            }
            showAlert("🗑️ Transaction removed from Cloud.", "info")
        }
    }

    // ==========================================
    // PRICE TRACKER & MARGIN SYSTEM
    // ==========================================

    fun updateItemPrice(
        item: InventoryItemEntity,
        newMrp: Double,
        newWholesale: Double,
        newCost: Double,
        note: String,
        updatedBy: String
    ) {
        viewModelScope.launch {
            repository.updateItemPrice(
                item = item,
                newMrp = newMrp,
                newWholesale = newWholesale,
                newCost = newCost,
                note = note,
                updatedBy = updatedBy
            )
            showAlert("✅ Rates updated for ${item.displayName.ifBlank { item.name }} (Wholesale: ₹$newWholesale, MRP: ₹$newMrp)", "success")
        }
    }

    fun addNewItemWithPrice(
        name: String,
        category: String,
        unit: String,
        mrp: Double,
        wholesale: Double,
        cost: Double
    ) {
        viewModelScope.launch {
            val rootCat = category.trim().ifBlank { "General" }
            val itemName = name.trim()
            if (itemName.isBlank()) return@launch
            val key = if (rootCat == "root" || rootCat.isBlank()) itemName else "$rootCat>$itemName"
            val item = InventoryItemEntity(
                key = key,
                name = itemName,
                displayName = itemName,
                type = "item",
                parent = rootCat,
                allowedUnitsCsv = unit.ifBlank { "Box" },
                currentUnit = unit.ifBlank { "Box" },
                mrp = mrp,
                wholesalePrice = wholesale,
                costPrice = cost,
                previousMrp = mrp,
                previousWholesale = wholesale,
                lastPriceUpdated = System.currentTimeMillis()
            )
            repository.insertOrUpdateItem(item)
            if (mrp > 0 || wholesale > 0 || cost > 0) {
                repository.updateItemPrice(
                    item = item,
                    newMrp = mrp,
                    newWholesale = wholesale,
                    newCost = cost,
                    note = "Initial Price Entry",
                    updatedBy = _uiState.value.loggedInAdminName ?: "Admin"
                )
            }
            syncFolderAndItemTypes()
            showAlert("✅ '$itemName' added with prices to catalog!", "success")
        }
    }

    fun deletePriceHistory(id: Long) {
        viewModelScope.launch {
            repository.deletePriceHistory(id)
            showAlert("🗑️ Price history record deleted.", "info")
        }
    }

    // ==========================================
    // 3-TIER GRANULAR PERMISSIONS & MASTER CONFIG
    // ==========================================

    fun updateDevicePermissions(targetEmail: String, permissions: com.example.data.AdminPermissions) {
        viewModelScope.launch {
            val operator = _uiState.value.loggedInAdminEmail ?: adminAuthManager.getLocalMasterAdminEmail()
            val result = adminAuthManager.updateAdminPermissions(
                operatorEmail = operator,
                targetEmail = targetEmail,
                permissions = permissions
            )
            if (result.isSuccess) {
                showAlert("✅ Permissions updated for $targetEmail", "success")
            } else {
                showAlert("❌ Failed: ${result.exceptionOrNull()?.message}", "error")
            }
        }
    }

    fun updateMasterGlobalConfig(
        isPriceGlobal: Boolean,
        isKhataGlobal: Boolean,
        isStaffGlobal: Boolean,
        isInventoryGlobal: Boolean
    ) {
        viewModelScope.launch {
            val operator = _uiState.value.loggedInAdminEmail ?: adminAuthManager.getLocalMasterAdminEmail()
            val result = adminAuthManager.updateGlobalFeatureConfig(
                operatorEmail = operator,
                isPriceGlobal = isPriceGlobal,
                isKhataGlobal = isKhataGlobal,
                isStaffGlobal = isStaffGlobal,
                isInventoryGlobal = isInventoryGlobal
            )
            if (result.isSuccess) {
                showAlert("👑 Global broadcast toggles updated!", "success")
            } else {
                showAlert("❌ Failed: ${result.exceptionOrNull()?.message}", "error")
            }
        }
    }

    // ==========================================
    // KHATA STICKY MEMOS & QUICK NOTIFICATIONS
    // ==========================================

    fun setKhataMemosOpen(open: Boolean) {
        _uiState.update { it.copy(isKhataMemosOpen = open) }
    }

    fun addKhataMemo(note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            val memoId = repository.insertKhataMemo(note.trim())
            val memo = com.example.data.KhataMemoEntity(id = memoId, note = note.trim(), timestamp = System.currentTimeMillis())
            khataCloudManager.uploadMemoToCloud(memo)
            showAlert("✅ Saved note to App Database & Cloud!", "success")
        }
    }

    fun deleteKhataMemo(id: Long) {
        viewModelScope.launch {
            repository.deleteKhataMemo(id)
            khataCloudManager.deleteMemoFromCloud(id)
            showAlert("🗑️ Notification removed", "info")
        }
    }

    fun clearAllKhataMemos() {
        viewModelScope.launch {
            repository.clearAllKhataMemos()
            showAlert("🗑️ All notifications cleared", "info")
        }
    }

    fun toggleStickyBottomMemoBar(enabled: Boolean) {
        adminAuthManager.setStickyBottomMemoBarEnabled(enabled)
        _uiState.update { it.copy(isStickyBottomMemoBarEnabled = enabled) }
        showAlert(if (enabled) "📌 Khata memo bar enabled" else "📌 Khata memo bar hidden", "info")
    }
}
