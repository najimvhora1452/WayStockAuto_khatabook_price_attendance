package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class StockCloudManager(
    private val context: Context,
    private val repository: WayStockRepository
) {
    companion object {
        private const val TAG = "StockCloudManager"
        private const val STOCK_COLLECTION = "stock"
        private const val ROOT_STRUCTURES_DOC = "rootStructures"
        private const val APP_SETTINGS_COLLECTION = "appSettings"
        private const val VERSION_CONTROL_DOC = "versionControl"
        private const val GLOBAL_NOTIFICATION_DOC = "globalNotification"
        private const val ADMIN_AUTH_DOC = "adminAuth"
        private const val PREFS_NAME = "waystock_cloud_prefs"
        private const val KEY_LOCAL_TOKEN = "wayStock_local_token"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var syncDebounceJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _latestBroadcast = MutableStateFlow<Pair<String, Long>?>(null)
    val latestBroadcast: StateFlow<Pair<String, Long>?> = _latestBroadcast.asStateFlow()

    private val _cloudAdminPassword = MutableStateFlow<String?>(null)
    val cloudAdminPassword: StateFlow<String?> = _cloudAdminPassword.asStateFlow()

    private var onInventoryUpdatedCallback: (() -> Unit)? = null

    fun setOnInventoryUpdatedCallback(callback: () -> Unit) {
        this.onInventoryUpdatedCallback = callback
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                null
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore instance error: ${e.message}")
            null
        }
    }

    private fun getLocalToken(): String {
        return prefs.getString(KEY_LOCAL_TOKEN, "") ?: ""
    }

    private fun setLocalToken(token: String) {
        prefs.edit().putString(KEY_LOCAL_TOKEN, token).apply()
    }

    /**
     * Start real-time sync with Firebase Firestore for Stock & App Settings
     */
    fun startRealtimeSync() {
        val firestore = getFirestore() ?: return

        stopSync()
        _isSyncing.value = true

        // 1. Initial Pull of Inventory from Cloud
        scope.launch {
            pullFullInventoryFromCloud()
        }

        try {
            // 2. Watch Version Control Token (triggers re-pull on mutation)
            val tokenListener = firestore.collection(APP_SETTINGS_COLLECTION)
                .document(VERSION_CONTROL_DOC)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.d(TAG, "Version token listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val cloudToken = snapshot.get("token")?.toString() ?: ""
                        val localToken = getLocalToken()

                        if (cloudToken.isNotBlank() && cloudToken != localToken) {
                            Log.i(TAG, "⚡ Background cloud inventory change detected (Token: $cloudToken vs $localToken). Pulling...")
                            scope.launch {
                                pullFullInventoryFromCloud(cloudToken)
                            }
                        }
                    }
                }
            activeListeners.add(tokenListener)

            // 3. Watch Global Broadcast Notifications
            val notifListener = firestore.collection(APP_SETTINGS_COLLECTION)
                .document(GLOBAL_NOTIFICATION_DOC)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val text = snapshot.getString("text") ?: ""
                        val timestamp = snapshot.getLong("timestamp") ?: 0L
                        if (text.isNotBlank()) {
                            _latestBroadcast.value = Pair(text, timestamp)
                        }
                    }
                }
            activeListeners.add(notifListener)

            // 4. Watch Admin Auth (Master Password)
            val authListener = firestore.collection(APP_SETTINGS_COLLECTION)
                .document(ADMIN_AUTH_DOC)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val pass = snapshot.get("password")?.toString()
                        if (!pass.isNullOrBlank()) {
                            _cloudAdminPassword.value = pass
                        }
                    }
                }
            activeListeners.add(authListener)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting StockCloudManager listeners: ${e.message}")
        }
    }

    /**
     * Pulls the rootStructures and all segment documents from Firestore
     * and saves them into the Room local database.
     */
    suspend fun pullFullInventoryFromCloud(expectedToken: String? = null) {
        val firestore = getFirestore() ?: return
        try {
            _isSyncing.value = true
            Log.i(TAG, "☁️ Pulling full inventory from Firestore collection '$STOCK_COLLECTION'...")

            val rootDocSnap = firestore.collection(STOCK_COLLECTION)
                .document(ROOT_STRUCTURES_DOC)
                .get()
                .await()

            if (!rootDocSnap.exists()) {
                Log.w(TAG, "Root structures doc '$ROOT_STRUCTURES_DOC' not found in Firestore.")
                _isSyncing.value = false
                return
            }

            val rootData = rootDocSnap.data ?: emptyMap<String, Any>()
            val parsedItems = mutableMapOf<String, InventoryItemEntity>()

            // 1. Parse all items directly from rootStructures
            for ((key, rawValue) in rootData) {
                if (rawValue is Map<*, *>) {
                    val item = parseItemMap(key, rawValue)
                    if (item != null) {
                        parsedItems[key] = item
                    }
                }
            }

            // 2. Fetch all segmented docs corresponding to root keys
            val rootKeys = parsedItems.keys.toList()
            val segmentJobs = rootKeys.map { rootKey ->
                val segmentDocId = "segment_${rootKey.replace(" ", "_")}"
                firestore.collection(STOCK_COLLECTION)
                    .document(segmentDocId)
                    .get()
            }

            for (task in segmentJobs) {
                try {
                    val snap = task.await()
                    if (snap.exists()) {
                        val segmentData = snap.data ?: emptyMap<String, Any>()
                        for ((key, rawValue) in segmentData) {
                            if (rawValue is Map<*, *>) {
                                val item = parseItemMap(key, rawValue)
                                if (item != null) {
                                    parsedItems[key] = item
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to pull segment: ${e.message}")
                }
            }

            if (parsedItems.isNotEmpty()) {
                val itemList = parsedItems.values.toList()
                Log.i(TAG, "✅ Pulled ${itemList.size} inventory items from Firestore. Updating Room DB...")
                repository.replaceAllInventory(itemList)

                // Update token
                val finalToken = expectedToken ?: System.currentTimeMillis().toString()
                setLocalToken(finalToken)

                withContext(Dispatchers.Main) {
                    onInventoryUpdatedCallback?.invoke()
                }
            } else {
                Log.i(TAG, "No inventory items found on cloud.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling inventory from cloud: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    private fun parseItemMap(key: String, map: Map<*, *>): InventoryItemEntity? {
        try {
            val name = map["name"]?.toString() ?: key.substringAfterLast(">")
            val displayName = map["displayName"]?.toString() ?: name
            var type = map["type"]?.toString() ?: "item"
            val parent = map["parent"]?.toString() ?: if (key.contains(">")) key.substringBeforeLast(">") else "root"
            val toggleOn = when (val t = map["toggleOn"]) {
                is Boolean -> t
                is String -> t.toBoolean()
                else -> false
            }

            // Allowed units
            val unitsList = when (val u = map["allowedUnits"]) {
                is List<*> -> u.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                is String -> u.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> listOf("Box", "Packet", "Bunch", "Kg")
            }
            val allowedUnitsCsv = if (unitsList.isNotEmpty()) unitsList.joinToString(",") else "Box"
            val currentUnit = map["currentUnit"]?.toString()?.ifBlank { unitsList.firstOrNull() ?: "Box" } ?: "Box"

            val children = when (val c = map["children"]) {
                is List<*> -> c.mapNotNull { it?.toString() }
                else -> emptyList()
            }
            if (children.isNotEmpty()) {
                type = "folder"
            }

            val mrp = when (val v = map["mrp"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val wholesale = when (val v = map["wholesalePrice"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val cost = when (val v = map["costPrice"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val lastPriceUpdated = when (val v = map["lastPriceUpdated"]) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull() ?: 0L
                else -> 0L
            }
            val priceNote = map["priceNote"]?.toString() ?: ""

            return InventoryItemEntity(
                key = key,
                name = name,
                displayName = displayName,
                type = type,
                parent = parent,
                toggleOn = toggleOn,
                allowedUnitsCsv = allowedUnitsCsv,
                currentUnit = currentUnit,
                mrp = mrp,
                wholesalePrice = wholesalePrice(mrp, wholesale),
                costPrice = cost,
                lastPriceUpdated = lastPriceUpdated,
                priceNote = priceNote
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing item map for $key: ${e.message}")
            return null
        }
    }

    private fun wholesalePrice(mrp: Double, wholesale: Double): Double {
        return if (wholesale > 0.0) wholesale else 0.0
    }

    /**
     * Debounced cloud sync when Admin modifies inventory in the Android app.
     * Segregates items into rootStructures and segment_* documents.
     */
    fun scheduleSyncToFirebase() {
        syncDebounceJob?.cancel()
        syncDebounceJob = scope.launch {
            delay(1200) // 1.2 second debounce like web debounce engine
            syncToFirebaseImmediate()
        }
    }

    /**
     * Immediate push of all current local inventory to Firestore
     */
    suspend fun syncToFirebaseImmediate(): Boolean {
        val firestore = getFirestore() ?: return false
        return try {
            _isSyncing.value = true
            val items = repository.getAllInventoryList()
            Log.i(TAG, "☁️ Committing ${items.size} inventory items to Firebase Cloud...")

            val rootStructures = mutableMapOf<String, Any>()
            val segmentedDocs = mutableMapOf<String, MutableMap<String, Any>>()

            // Group children for each parent
            val childrenMap = mutableMapOf<String, MutableList<String>>()
            items.forEach { item ->
                if (item.parent.isNotBlank() && item.parent != "root") {
                    childrenMap.getOrPut(item.parent) { mutableListOf() }.add(item.key)
                }
            }

            items.forEach { item ->
                val itemChildren = childrenMap[item.key] ?: emptyList()
                val isFolder = item.type == "folder" || itemChildren.isNotEmpty()
                val units = item.allowedUnitsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }

                val itemMap = hashMapOf<String, Any>(
                    "name" to item.name,
                    "displayName" to item.displayName.ifBlank { item.name },
                    "type" to if (isFolder) "folder" else "item",
                    "parent" to item.parent,
                    "toggleOn" to item.toggleOn,
                    "allowedUnits" to units,
                    "currentUnit" to item.currentUnit,
                    "children" to itemChildren
                )

                if (item.mrp > 0.0) itemMap["mrp"] = item.mrp
                if (item.wholesalePrice > 0.0) itemMap["wholesalePrice"] = item.wholesalePrice
                if (item.costPrice > 0.0) itemMap["costPrice"] = item.costPrice
                if (item.lastPriceUpdated > 0L) itemMap["lastPriceUpdated"] = item.lastPriceUpdated
                if (item.priceNote.isNotBlank()) itemMap["priceNote"] = item.priceNote

                if (item.parent == "root") {
                    rootStructures[item.key] = itemMap
                    if (!segmentedDocs.containsKey(item.key)) {
                        segmentedDocs[item.key] = mutableMapOf()
                    }
                } else {
                    val rootParent = item.key.substringBefore(">").trim()
                    val segmentMap = segmentedDocs.getOrPut(rootParent) { mutableMapOf() }
                    segmentMap[item.key] = itemMap
                }
            }

            // 1. Upload rootStructures document
            firestore.collection(STOCK_COLLECTION)
                .document(ROOT_STRUCTURES_DOC)
                .set(rootStructures)
                .await()

            // 2. Upload each segmented document
            for ((rootKey, segmentMap) in segmentedDocs) {
                val segmentDocId = "segment_${rootKey.replace(" ", "_")}"
                firestore.collection(STOCK_COLLECTION)
                    .document(segmentDocId)
                    .set(segmentMap)
                    .await()
            }

            // 3. Update versionControl token
            val newToken = System.currentTimeMillis().toString()
            firestore.collection(APP_SETTINGS_COLLECTION)
                .document(VERSION_CONTROL_DOC)
                .set(mapOf("token" to newToken), SetOptions.merge())
                .await()

            setLocalToken(newToken)
            Log.i(TAG, "🎯 Segmented Cloud Sync Complete. New token: $newToken")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Multi-doc segment push to Firebase failed: ${e.message}")
            false
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Updates Cloud Admin Master Password in appSettings/adminAuth
     */
    suspend fun updateCloudAdminPassword(newPassword: String): Result<Unit> {
        val firestore = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            firestore.collection(APP_SETTINGS_COLLECTION)
                .document(ADMIN_AUTH_DOC)
                .set(mapOf("password" to newPassword), SetOptions.merge())
                .await()
            _cloudAdminPassword.value = newPassword
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Broadcasts notification to all users via appSettings/globalNotification
     */
    suspend fun sendGlobalBroadcast(message: String): Result<Unit> {
        val firestore = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            firestore.collection(APP_SETTINGS_COLLECTION)
                .document(GLOBAL_NOTIFICATION_DOC)
                .set(
                    mapOf(
                        "text" to message,
                        "timestamp" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopSync() {
        for (l in activeListeners) {
            try {
                l.remove()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeListeners.clear()
        _isSyncing.value = false
    }
}
