package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AdminAuthManager(private val context: Context) {

    companion object {
        const val SUPER_ADMIN_EMAIL = "najimvhora1452@gmail.com"
        private const val SETTINGS_COLLECTION = "system_settings"
        private const val MASTER_CONFIG_DOC = "master_security"
        private const val ADMIN_DEVICES_COLLECTION = "admin_devices"
        private const val PREFS_NAME = "waystock_admin_prefs"
        private const val KEY_LOCAL_AUTO_LAUNCH = "local_auto_launch_enabled"
        private const val KEY_LOCAL_ADMIN_EMAIL = "local_admin_email"
        private const val KEY_LOCAL_ADMIN_NAME = "local_admin_name"
        private const val KEY_LOCAL_USER_NAME = "local_user_name"
        private const val KEY_LOCAL_USER_ID = "local_user_id"
        private const val KEY_LOCAL_IS_ONBOARDED = "local_is_onboarded"
        private const val KEY_STICKY_MEMO_BAR_ENABLED = "sticky_memo_bar_enabled"
        private const val TAG = "AdminAuthManager"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLocalUserOnboarded(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_IS_ONBOARDED, false)
    }

    fun getLocalUserProfile(): Pair<String, String> {
        val name = prefs.getString(KEY_LOCAL_USER_NAME, "") ?: ""
        val id = prefs.getString(KEY_LOCAL_USER_ID, "guest") ?: "guest"
        return Pair(name, id)
    }

    fun setLocalUserProfile(name: String, userId: String) {
        prefs.edit().apply {
            putString(KEY_LOCAL_USER_NAME, name)
            putString(KEY_LOCAL_USER_ID, userId)
            putBoolean(KEY_LOCAL_IS_ONBOARDED, true)
            apply()
        }
    }

    fun isLocalAutoLaunchEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_AUTO_LAUNCH, false)
    }

    fun setLocalAutoLaunchEnabled(enabled: Boolean, email: String? = null, name: String? = null) {
        prefs.edit().apply {
            putBoolean(KEY_LOCAL_AUTO_LAUNCH, enabled)
            if (email != null) putString(KEY_LOCAL_ADMIN_EMAIL, email)
            if (name != null) putString(KEY_LOCAL_ADMIN_NAME, name)
            if (!enabled) {
                remove(KEY_LOCAL_AUTO_LAUNCH)
            }
            apply()
        }
    }

    fun getLocalAdminProfile(): Pair<String?, String?> {
        val email = prefs.getString(KEY_LOCAL_ADMIN_EMAIL, null)
        val name = prefs.getString(KEY_LOCAL_ADMIN_NAME, null)
        return Pair(email, name)
    }

    fun isStickyBottomMemoBarEnabled(): Boolean {
        return prefs.getBoolean(KEY_STICKY_MEMO_BAR_ENABLED, true)
    }

    fun setStickyBottomMemoBarEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STICKY_MEMO_BAR_ENABLED, enabled).apply()
    }

    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return null
            }
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                    .build()
                firestore.firestoreSettings = settings
            } catch (_: Exception) {}
            firestore
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available: ${e.message}")
            null
        }
    }

    private fun getAuthInstance(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return null
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Auth not available: ${e.message}")
            null
        }
    }

    private val credentialManager by lazy { CredentialManager.create(context) }

    val currentFirebaseUser get() = getAuthInstance()?.currentUser

    fun isSuperAdmin(email: String?): Boolean {
        return email?.trim()?.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) == true
    }

    /**
     * Google Sign-In using Android Credential Manager & GoogleId
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Pair<String, String>> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId("43806944793-dummy.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")

                // Authenticate with Firebase Auth if token and auth available
                try {
                    getAuthInstance()?.let { fbAuth ->
                        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                        fbAuth.signInWithCredential(firebaseCred).await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Auth with credential failed, continuing with email: ${e.message}")
                }

                Result.success(Pair(email, displayName))
            } else {
                Result.failure(Exception("Unknown credential format"))
            }
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            Log.w(TAG, "No Google accounts found on this device/emulator: ${e.message}")
            Result.failure(Exception("No Google account configured on device/emulator"))
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            Log.i(TAG, "Google Sign-in cancelled by user")
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            Log.w(TAG, "Google Sign-in notice: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Direct Super Admin Login (Instant bypass for testing/emulator)
     */
    fun directSuperAdminLogin(): Pair<String, String> {
        return Pair(SUPER_ADMIN_EMAIL, "Master Administrator")
    }

    /**
     * Sign out current admin account
     */
    suspend fun signOut() {
        try {
            setLocalAutoLaunchEnabled(false)
            getAuthInstance()?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
    }

    /**
     * Stream master security config (PIN, Last Modified By, and Global Feature Flags)
     */
    fun getMasterSecurityConfig(): Flow<MasterSecurityConfig> {
        val db = getFirestoreInstance() ?: return flow {
            // Offline fallback
            val isPriceGlobal = prefs.getBoolean("global_price_page", false)
            val isKhataGlobal = prefs.getBoolean("global_khata_page", true)
            val isStaffGlobal = prefs.getBoolean("global_staff_page", true)
            val isInvGlobal = prefs.getBoolean("global_inv_edit", true)
            val pin = prefs.getString("local_master_pin", "1234") ?: "1234"
            emit(
                MasterSecurityConfig(
                    masterAdminEmail = SUPER_ADMIN_EMAIL,
                    masterPin = pin,
                    lastModifiedBy = SUPER_ADMIN_EMAIL,
                    lastModifiedAt = System.currentTimeMillis(),
                    isPricePageGlobalToPremium = isPriceGlobal,
                    isKhataGlobalToPremium = isKhataGlobal,
                    isStaffGlobalToPremium = isStaffGlobal,
                    isInventoryEditGlobalToPremium = isInvGlobal
                )
            )
        }
        return callbackFlow {
            val listener = db.collection(SETTINGS_COLLECTION)
                .document(MASTER_CONFIG_DOC)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen note for security config: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val pin = snapshot.getString("masterPin") ?: "1234"
                        val lastModBy = snapshot.getString("lastModifiedBy") ?: SUPER_ADMIN_EMAIL
                        val lastModAt = snapshot.getLong("lastModifiedAt") ?: 0L
                        val isPriceGlobal = snapshot.getBoolean("isPricePageGlobalToPremium") ?: false
                        val isKhataGlobal = snapshot.getBoolean("isKhataGlobalToPremium") ?: true
                        val isStaffGlobal = snapshot.getBoolean("isStaffGlobalToPremium") ?: true
                        val isInvGlobal = snapshot.getBoolean("isInventoryEditGlobalToPremium") ?: true
                        trySend(
                            MasterSecurityConfig(
                                masterAdminEmail = SUPER_ADMIN_EMAIL,
                                masterPin = pin,
                                lastModifiedBy = lastModBy,
                                lastModifiedAt = lastModAt,
                                isPricePageGlobalToPremium = isPriceGlobal,
                                isKhataGlobalToPremium = isKhataGlobal,
                                isStaffGlobalToPremium = isStaffGlobal,
                                isInventoryEditGlobalToPremium = isInvGlobal
                            )
                        )
                    } else {
                        trySend(MasterSecurityConfig())
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Update master PIN - strictly allowed only if executed by Super Admin
     */
    suspend fun updateMasterPin(operatorEmail: String, newPin: String): Result<Boolean> {
        if (!isSuperAdmin(operatorEmail)) {
            return Result.failure(SecurityException("Only Super Admin ($SUPER_ADMIN_EMAIL) can update the Master PIN!"))
        }

        prefs.edit().putString("local_master_pin", newPin).apply()
        val db = getFirestoreInstance() ?: return Result.success(true)

        return try {
            val data = mapOf(
                "masterAdminEmail" to SUPER_ADMIN_EMAIL,
                "masterPin" to newPin,
                "lastModifiedBy" to operatorEmail,
                "lastModifiedAt" to System.currentTimeMillis()
            )
            db.collection(SETTINGS_COLLECTION)
                .document(MASTER_CONFIG_DOC)
                .set(data, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update master PIN: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update Global Feature Broadcast - Super Admin toggles which features all Premium admins get
     */
    suspend fun updateGlobalFeatureConfig(
        operatorEmail: String,
        isPriceGlobal: Boolean,
        isKhataGlobal: Boolean,
        isStaffGlobal: Boolean,
        isInventoryGlobal: Boolean
    ): Result<Boolean> {
        if (!isSuperAdmin(operatorEmail)) {
            return Result.failure(SecurityException("Only Super Admin ($SUPER_ADMIN_EMAIL) can change global feature toggles!"))
        }

        prefs.edit().apply {
            putBoolean("global_price_page", isPriceGlobal)
            putBoolean("global_khata_page", isKhataGlobal)
            putBoolean("global_staff_page", isStaffGlobal)
            putBoolean("global_inv_edit", isInventoryGlobal)
            apply()
        }

        val db = getFirestoreInstance() ?: return Result.success(true)

        return try {
            val data = mapOf(
                "isPricePageGlobalToPremium" to isPriceGlobal,
                "isKhataGlobalToPremium" to isKhataGlobal,
                "isStaffGlobalToPremium" to isStaffGlobal,
                "isInventoryEditGlobalToPremium" to isInventoryGlobal,
                "lastModifiedBy" to operatorEmail,
                "lastModifiedAt" to System.currentTimeMillis()
            )
            db.collection(SETTINGS_COLLECTION)
                .document(MASTER_CONFIG_DOC)
                .set(data, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update global config: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Stream all registered admin devices with auto-launch toggle and permissions
     */
    fun getAllAdminDevices(): Flow<List<AdminDeviceEntity>> {
        val db = getFirestoreInstance() ?: return flow {
            // Local fallback seed so Super Admin and sample admins are visible
            val superAdmin = AdminDeviceEntity(
                email = SUPER_ADMIN_EMAIL,
                displayName = "Najim Vhora",
                isAutoLaunchEnabled = true,
                registeredAt = System.currentTimeMillis() - 864000000L,
                lastActiveAt = System.currentTimeMillis(),
                isSuperAdmin = true,
                permissions = AdminPermissions(
                    canAccessPricePage = true,
                    canManageInventory = true,
                    canManageKhata = true,
                    canManageStaff = true,
                    canSendBroadcast = true,
                    canExportReports = true
                )
            )
            val staffAdmin = AdminDeviceEntity(
                email = "rahul.staff@example.com",
                displayName = "Rahul (Staff Admin)",
                isAutoLaunchEnabled = true,
                registeredAt = System.currentTimeMillis() - 432000000L,
                lastActiveAt = System.currentTimeMillis() - 3600000L,
                isSuperAdmin = false,
                permissions = AdminPermissions(
                    canAccessPricePage = false,
                    canManageInventory = true,
                    canManageKhata = true,
                    canManageStaff = true,
                    canSendBroadcast = false,
                    canExportReports = false
                )
            )
            emit(listOf(superAdmin, staffAdmin))
        }
        return callbackFlow {
            val listener = db.collection(ADMIN_DEVICES_COLLECTION)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Note fetching admin devices: ${error.message}")
                        return@addSnapshotListener
                    }
                    val list = snapshots?.documents?.mapNotNull { doc ->
                        val email = doc.getString("email") ?: doc.id
                        val name = doc.getString("displayName") ?: email.substringBefore("@")
                        val enabled = doc.getBoolean("isAutoLaunchEnabled") ?: false
                        val regAt = doc.getLong("registeredAt") ?: 0L
                        val lastActAt = doc.getLong("lastActiveAt") ?: 0L
                        val isSuper = isSuperAdmin(email)

                        val permMap = doc.get("permissions") as? Map<*, *>
                        val perms = if (isSuper) {
                            AdminPermissions(
                                canAccessPricePage = true,
                                canManageInventory = true,
                                canManageKhata = true,
                                canManageStaff = true,
                                canSendBroadcast = true,
                                canExportReports = true
                            )
                        } else {
                            AdminPermissions(
                                canAccessPricePage = (permMap?.get("canAccessPricePage") as? Boolean) ?: false,
                                canManageInventory = (permMap?.get("canManageInventory") as? Boolean) ?: true,
                                canManageKhata = (permMap?.get("canManageKhata") as? Boolean) ?: true,
                                canManageStaff = (permMap?.get("canManageStaff") as? Boolean) ?: true,
                                canSendBroadcast = (permMap?.get("canSendBroadcast") as? Boolean) ?: false,
                                canExportReports = (permMap?.get("canExportReports") as? Boolean) ?: true
                            )
                        }

                        AdminDeviceEntity(
                            email = email,
                            displayName = name,
                            isAutoLaunchEnabled = enabled,
                            registeredAt = regAt,
                            lastActiveAt = lastActAt,
                            isSuperAdmin = isSuper,
                            permissions = perms
                        )
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Toggle or register auto-launch for a specific logged-in Google account
     */
    suspend fun setAutoLaunchForUser(
        email: String,
        displayName: String,
        isEnabled: Boolean
    ): Result<Boolean> {
        val isSuper = isSuperAdmin(email)
        val defaultPerms = if (isSuper) {
            AdminPermissions(
                canAccessPricePage = true,
                canManageInventory = true,
                canManageKhata = true,
                canManageStaff = true,
                canSendBroadcast = true,
                canExportReports = true
            )
        } else {
            AdminPermissions()
        }

        val db = getFirestoreInstance() ?: return Result.success(true)

        return try {
            val docId = email.trim().replace(".", "_").replace("@", "_at_")
            val data = mapOf(
                "email" to email.trim(),
                "displayName" to displayName.trim(),
                "isAutoLaunchEnabled" to isEnabled,
                "registeredAt" to System.currentTimeMillis(),
                "lastActiveAt" to System.currentTimeMillis(),
                "isSuperAdmin" to isSuper,
                "permissions" to mapOf(
                    "canAccessPricePage" to defaultPerms.canAccessPricePage,
                    "canManageInventory" to defaultPerms.canManageInventory,
                    "canManageKhata" to defaultPerms.canManageKhata,
                    "canManageStaff" to defaultPerms.canManageStaff,
                    "canSendBroadcast" to defaultPerms.canSendBroadcast,
                    "canExportReports" to defaultPerms.canExportReports
                )
            )
            db.collection(ADMIN_DEVICES_COLLECTION)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set auto-launch for $email: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Super Admin updates granular permissions for a specific Premium Admin
     */
    suspend fun updateAdminPermissions(
        operatorEmail: String,
        targetEmail: String,
        permissions: AdminPermissions
    ): Result<Boolean> {
        if (!isSuperAdmin(operatorEmail)) {
            return Result.failure(SecurityException("Only Super Admin can edit user permissions!"))
        }

        val db = getFirestoreInstance() ?: return Result.success(true)

        return try {
            val docId = targetEmail.trim().replace(".", "_").replace("@", "_at_")
            val permMap = mapOf(
                "canAccessPricePage" to permissions.canAccessPricePage,
                "canManageInventory" to permissions.canManageInventory,
                "canManageKhata" to permissions.canManageKhata,
                "canManageStaff" to permissions.canManageStaff,
                "canSendBroadcast" to permissions.canSendBroadcast,
                "canExportReports" to permissions.canExportReports
            )
            db.collection(ADMIN_DEVICES_COLLECTION)
                .document(docId)
                .update("permissions", permMap)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update permissions for $targetEmail: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remote Toggle or Revoke: Super Admin can disable/enable any device
     */
    suspend fun setDeviceEnabledBySuperAdmin(
        targetEmail: String,
        isEnabled: Boolean
    ): Result<Boolean> {
        val db = getFirestoreInstance() ?: return Result.success(true)

        return try {
            val docId = targetEmail.trim().replace(".", "_").replace("@", "_at_")
            db.collection(ADMIN_DEVICES_COLLECTION)
                .document(docId)
                .update("isAutoLaunchEnabled", isEnabled)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle remote admin for $targetEmail: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete an admin record from the list
     */
    suspend fun deleteAdminDevice(targetEmail: String): Result<Boolean> {
        val db = getFirestoreInstance() ?: return Result.failure(IllegalStateException("Cloud database is currently offline."))

        return try {
            val docId = targetEmail.trim().replace(".", "_").replace("@", "_at_")
            db.collection(ADMIN_DEVICES_COLLECTION)
                .document(docId)
                .delete()
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete admin device $targetEmail: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Direct launch check on app start with live internet
     */
    suspend fun checkAutoLaunchEligibility(email: String): Boolean {
        if (email.isBlank()) return false
        val db = getFirestoreInstance() ?: return false
        return try {
            val docId = email.trim().replace(".", "_").replace("@", "_at_")
            val doc = db.collection(ADMIN_DEVICES_COLLECTION)
                .document(docId)
                .get()
                .await()
            if (doc.exists()) {
                doc.getBoolean("isAutoLaunchEnabled") == true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-launch check note: ${e.message}")
            false
        }
    }
}
