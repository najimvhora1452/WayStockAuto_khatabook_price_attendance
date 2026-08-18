package com.example.data

data class AdminPermissions(
    val canAccessPricePage: Boolean = false,
    val canManageInventory: Boolean = true,
    val canManageKhata: Boolean = true,
    val canManageStaff: Boolean = true,
    val canSendBroadcast: Boolean = false,
    val canExportReports: Boolean = true
)

data class AdminDeviceEntity(
    val email: String = "",
    val displayName: String = "",
    val isAutoLaunchEnabled: Boolean = false,
    val registeredAt: Long = 0L,
    val lastActiveAt: Long = 0L,
    val isSuperAdmin: Boolean = false,
    val permissions: AdminPermissions = AdminPermissions()
)

data class MasterSecurityConfig(
    val masterAdminEmail: String = "najimvhora1452@gmail.com",
    val masterPin: String = "1234",
    val lastModifiedBy: String = "najimvhora1452@gmail.com",
    val lastModifiedAt: Long = 0L,
    // Global broadcast feature toggles for all Premium users
    val isPricePageGlobalToPremium: Boolean = false,
    val isKhataGlobalToPremium: Boolean = true,
    val isStaffGlobalToPremium: Boolean = true,
    val isInventoryEditGlobalToPremium: Boolean = true
)
