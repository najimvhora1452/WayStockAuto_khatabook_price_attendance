package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WayStockRepository(context: Context) {
    private val db = WayStockDatabase.getDatabase(context)
    private val dao = db.inventoryDao()

    val allInventoryItems: Flow<List<InventoryItemEntity>> = dao.getAllInventoryItemsFlow()

    fun getInventoryItemsByParent(parentKey: String): Flow<List<InventoryItemEntity>> {
        return dao.getInventoryItemsByParent(parentKey)
    }

    fun getCartItems(userId: String): Flow<List<CartItemEntity>> {
        return dao.getCartItemsFlow(userId)
    }

    fun getSearchHistory(userId: String): Flow<List<SearchHistoryEntity>> {
        return dao.getSearchHistoryFlow(userId)
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = dao.getAllInventoryItemsFlow().first()
        if (existing.isEmpty()) {
            val defaultItems = listOf(
                // Folders at Root
                InventoryItemEntity(key = "Snacks", name = "Snacks", displayName = "Snacks", type = "folder", parent = "root"),
                InventoryItemEntity(key = "Paan Masala", name = "Paan Masala", displayName = "Paan Masala", type = "folder", parent = "root"),
                InventoryItemEntity(key = "Cigarettes", name = "Cigarettes", displayName = "Cigarettes", type = "folder", parent = "root"),
                InventoryItemEntity(key = "Perfumes", name = "Perfumes", displayName = "Perfumes", type = "folder", parent = "root"),
                InventoryItemEntity(key = "Beverages", name = "Beverages", displayName = "Beverages", type = "folder", parent = "root"),

                // Snacks sub-items & folders
                InventoryItemEntity(key = "Snacks>Lays", name = "Lays", displayName = "Lays", type = "folder", parent = "Snacks"),
                InventoryItemEntity(key = "Snacks>Bingo", name = "Bingo", displayName = "Bingo", type = "item", parent = "Snacks", allowedUnitsCsv = "Box,Packet,Carton", mrp = 20.0, wholesalePrice = 16.5, costPrice = 15.0, previousMrp = 20.0, previousWholesale = 16.5, lastPriceUpdated = System.currentTimeMillis()),
                InventoryItemEntity(key = "Snacks>Pringles", name = "Pringles", displayName = "Pringles", type = "item", parent = "Snacks", allowedUnitsCsv = "Can,Box,Tray", mrp = 120.0, wholesalePrice = 98.0, costPrice = 90.0, previousMrp = 115.0, previousWholesale = 95.0, lastPriceUpdated = System.currentTimeMillis() - 86400000),

                // Snacks > Lays sub-items
                InventoryItemEntity(key = "Snacks>Lays>Magic Masala", name = "Magic Masala", displayName = "Magic Masala", type = "item", parent = "Snacks>Lays", allowedUnitsCsv = "Box,Packet", mrp = 20.0, wholesalePrice = 17.0, costPrice = 15.5, previousMrp = 20.0, previousWholesale = 17.0, lastPriceUpdated = System.currentTimeMillis()),
                InventoryItemEntity(key = "Snacks>Lays>Cream & Onion", name = "Cream & Onion", displayName = "Cream & Onion", type = "item", parent = "Snacks>Lays", allowedUnitsCsv = "Box,Packet", mrp = 20.0, wholesalePrice = 17.0, costPrice = 15.5, previousMrp = 20.0, previousWholesale = 17.0, lastPriceUpdated = System.currentTimeMillis()),
                InventoryItemEntity(key = "Snacks>Lays>Classic Salted", name = "Classic Salted", displayName = "Classic Salted", type = "item", parent = "Snacks>Lays", allowedUnitsCsv = "Box,Packet", mrp = 20.0, wholesalePrice = 17.0, costPrice = 15.5, previousMrp = 20.0, previousWholesale = 17.0, lastPriceUpdated = System.currentTimeMillis()),

                // Paan Masala sub-items
                InventoryItemEntity(key = "Paan Masala>Vimal", name = "Vimal", displayName = "Vimal", type = "item", parent = "Paan Masala", allowedUnitsCsv = "Pouch,Box,Packet", mrp = 5.0, wholesalePrice = 4.2, costPrice = 3.9, previousMrp = 5.0, previousWholesale = 4.0, lastPriceUpdated = System.currentTimeMillis() - 172800000),
                InventoryItemEntity(key = "Paan Masala>Rajnigandha", name = "Rajnigandha", displayName = "Rajnigandha", type = "item", parent = "Paan Masala", allowedUnitsCsv = "Can,Box,Pouch", mrp = 25.0, wholesalePrice = 21.5, costPrice = 20.0, previousMrp = 25.0, previousWholesale = 21.5, lastPriceUpdated = System.currentTimeMillis()),

                // Cigarettes sub-items
                InventoryItemEntity(key = "Cigarettes>Gold Flake", name = "Gold Flake", displayName = "Gold Flake", type = "item", parent = "Cigarettes", allowedUnitsCsv = "Packet,Carton,Box", mrp = 190.0, wholesalePrice = 172.0, costPrice = 165.0, previousMrp = 180.0, previousWholesale = 165.0, lastPriceUpdated = System.currentTimeMillis() - 259200000),
                InventoryItemEntity(key = "Cigarettes>Marlboro", name = "Marlboro", displayName = "Marlboro", type = "item", parent = "Cigarettes", allowedUnitsCsv = "Packet,Carton,Box", mrp = 360.0, wholesalePrice = 328.0, costPrice = 315.0, previousMrp = 350.0, previousWholesale = 320.0, lastPriceUpdated = System.currentTimeMillis() - 400000000),

                // Perfumes sub-items
                InventoryItemEntity(key = "Perfumes>Fogg", name = "Fogg", displayName = "Fogg", type = "item", parent = "Perfumes", allowedUnitsCsv = "Bottle,Box,Tray", mrp = 299.0, wholesalePrice = 220.0, costPrice = 195.0, previousMrp = 299.0, previousWholesale = 220.0, lastPriceUpdated = System.currentTimeMillis()),
                InventoryItemEntity(key = "Perfumes>Wild Stone", name = "Wild Stone", displayName = "Wild Stone", type = "item", parent = "Perfumes", allowedUnitsCsv = "Bottle,Box", mrp = 249.0, wholesalePrice = 185.0, costPrice = 165.0, previousMrp = 249.0, previousWholesale = 185.0, lastPriceUpdated = System.currentTimeMillis()),

                // Beverages sub-items
                InventoryItemEntity(key = "Beverages>Coca Cola", name = "Coca Cola", displayName = "Coca Cola", type = "item", parent = "Beverages", allowedUnitsCsv = "Bottle,Can,Crate", mrp = 40.0, wholesalePrice = 33.0, costPrice = 30.0, previousMrp = 40.0, previousWholesale = 33.0, lastPriceUpdated = System.currentTimeMillis()),
                InventoryItemEntity(key = "Beverages>Red Bull", name = "Red Bull", displayName = "Red Bull", type = "item", parent = "Beverages", allowedUnitsCsv = "Can,Tray,Box", mrp = 125.0, wholesalePrice = 105.0, costPrice = 98.0, previousMrp = 120.0, previousWholesale = 100.0, lastPriceUpdated = System.currentTimeMillis() - 500000000)
            )
            dao.insertAllItems(defaultItems)

            // Seed initial price history records
            val defaultHistory = listOf(
                PriceHistoryEntity(
                    itemKey = "Snacks>Pringles",
                    itemName = "Pringles",
                    oldMrp = 115.0,
                    newMrp = 120.0,
                    oldWholesale = 95.0,
                    newWholesale = 98.0,
                    oldCost = 88.0,
                    newCost = 90.0,
                    updatedBy = "Najim Vhora (Super Admin)",
                    timestamp = System.currentTimeMillis() - 86400000,
                    note = "Company price hike by distributor"
                ),
                PriceHistoryEntity(
                    itemKey = "Cigarettes>Gold Flake",
                    itemName = "Gold Flake",
                    oldMrp = 180.0,
                    newMrp = 190.0,
                    oldWholesale = 165.0,
                    newWholesale = 172.0,
                    oldCost = 158.0,
                    newCost = 165.0,
                    updatedBy = "Najim Vhora (Super Admin)",
                    timestamp = System.currentTimeMillis() - 259200000,
                    note = "Budget tax revision"
                )
            )
            for (h in defaultHistory) {
                dao.insertPriceHistory(h)
            }
        }
    }

    suspend fun insertOrUpdateItem(item: InventoryItemEntity) = dao.insertOrUpdateItem(item)

    suspend fun deleteItemAndChildren(key: String) = dao.deleteItemAndChildren(key)

    suspend fun searchInventory(query: String) = dao.searchInventory(query)

    suspend fun insertOrUpdateCartItem(item: CartItemEntity) = dao.insertOrUpdateCartItem(item)

    suspend fun deleteCartItem(key: String, userId: String) = dao.deleteCartItem(key, userId)

    suspend fun deleteCartItemsByKeys(key: String, rootFolder: String, userId: String) = dao.deleteCartItemsByKeys(key, rootFolder, userId)

    suspend fun clearCart(userId: String) = dao.clearCart(userId)

    suspend fun addSearchHistory(entry: SearchHistoryEntity) = dao.insertSearchHistory(entry)

    suspend fun getItemByKey(key: String): InventoryItemEntity? = dao.getItemByKey(key)

    val allRequestedItems: Flow<List<UserRequestedItemEntity>> = dao.getAllRequestedItemsFlow()

    suspend fun insertRequestedItem(item: UserRequestedItemEntity) = dao.insertRequestedItem(item)

    suspend fun deleteRequestedItem(id: Long) = dao.deleteRequestedItem(id)

    suspend fun clearAllRequestedItems() = dao.clearAllRequestedItems()

    // Staff and Attendance
    val allStaffMembers: Flow<List<StaffMemberEntity>> = dao.getAllStaffMembersFlow()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecordEntity>> = dao.getAttendanceForDateFlow(date)

    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>> = dao.getAttendanceForMonthFlow(monthPrefix)

    fun getAttendanceForStaff(staffId: String, staffName: String = ""): Flow<List<AttendanceRecordEntity>> {
        return if (staffName.isNotBlank()) {
            dao.getAttendanceForStaffOrNameFlow(staffId, staffName)
        } else {
            dao.getAttendanceForStaffFlow(staffId)
        }
    }

    suspend fun getStaffAttendanceForDate(staffId: String, date: String): AttendanceRecordEntity? = dao.getStaffAttendanceForDate(staffId, date)

    suspend fun insertOrUpdateStaffMember(staff: StaffMemberEntity) = dao.insertOrUpdateStaffMember(staff)

    suspend fun deleteStaffMember(staffId: String) = dao.deleteStaffMember(staffId)

    suspend fun insertOrUpdateAttendance(record: AttendanceRecordEntity) = dao.insertOrUpdateAttendance(record)

    suspend fun insertAllAttendance(records: List<AttendanceRecordEntity>) = dao.insertAllAttendance(records)

    suspend fun deleteAttendanceRecord(id: String) = dao.deleteAttendanceRecord(id)

    suspend fun seedDefaultStaffIfEmpty() {
        val existing = dao.getAllStaffMembersFlow().first()
        if (existing.isEmpty()) {
            val defaultStaff = listOf(
                StaffMemberEntity(id = "STAFF_1", name = "Najim Vhora", role = "Manager / Owner", phone = "+91 98765 43210", salaryType = "Monthly", monthlySalary = 30000.0, dailyWage = 1000.0),
                StaffMemberEntity(id = "STAFF_2", name = "Rahul Sharma", role = "Delivery Boy", phone = "+91 98234 56789", salaryType = "Monthly", monthlySalary = 15000.0, dailyWage = 500.0),
                StaffMemberEntity(id = "STAFF_3", name = "Sameer Khan", role = "Warehouse Helper", phone = "+91 97123 45678", salaryType = "Monthly", monthlySalary = 13500.0, dailyWage = 450.0),
                StaffMemberEntity(id = "STAFF_4", name = "Amit Patel", role = "Dispatch Lead", phone = "+91 96345 67890", salaryType = "Monthly", monthlySalary = 18000.0, dailyWage = 600.0)
            )
            dao.insertAllStaffMembers(defaultStaff)
        }
    }

    // Khata Book / Ledger repository methods
    val allKhataCustomers: Flow<List<KhataCustomerEntity>> = dao.getAllKhataCustomersFlow()
    val allKhataTransactions: Flow<List<KhataTransactionEntity>> = dao.getAllKhataTransactionsFlow()

    fun getTransactionsForCustomer(customerId: String): Flow<List<KhataTransactionEntity>> =
        dao.getTransactionsForCustomerFlow(customerId)

    suspend fun getLatestTransactionForCustomer(customerId: String): KhataTransactionEntity? =
        dao.getLatestTransactionForCustomer(customerId)

    suspend fun getKhataCustomerById(customerId: String): KhataCustomerEntity? =
        dao.getKhataCustomerById(customerId)

    suspend fun insertOrUpdateKhataCustomer(customer: KhataCustomerEntity) =
        dao.insertOrUpdateKhataCustomer(customer)

    suspend fun deleteKhataCustomer(customerId: String) {
        dao.deleteTransactionsForCustomer(customerId)
        dao.deleteKhataCustomer(customerId)
    }

    suspend fun addKhataTransaction(txn: KhataTransactionEntity) {
        dao.insertOrUpdateKhataTransaction(txn)
        // Recalculate customer balance: GAVE (Udhar Diya / +ve You will get), GOT (Jama Liya / -ve Reduces udhar)
        val customer = dao.getKhataCustomerById(txn.customerId)
        if (customer != null) {
            val delta = if (txn.type == "GAVE") txn.amount else -txn.amount
            val updatedBalance = customer.balance + delta
            dao.insertOrUpdateKhataCustomer(customer.copy(balance = updatedBalance, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteKhataTransaction(txn: KhataTransactionEntity) {
        dao.deleteKhataTransaction(txn.id)
        val customer = dao.getKhataCustomerById(txn.customerId)
        if (customer != null) {
            val delta = if (txn.type == "GAVE") -txn.amount else txn.amount
            val updatedBalance = customer.balance + delta
            dao.insertOrUpdateKhataCustomer(customer.copy(balance = updatedBalance, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun seedDefaultKhataIfEmpty() {
        val existing = dao.getAllKhataCustomersFlow().first()
        if (existing.isEmpty()) {
            val defaultCustomers = listOf(
                KhataCustomerEntity(
                    id = "CUST_1",
                    name = "Vikas General Store",
                    phone = "+91 98251 23456",
                    address = "Station Road Market",
                    customerType = "Customer",
                    balance = 4500.0 // You will get 4500
                ),
                KhataCustomerEntity(
                    id = "CUST_2",
                    name = "Ramesh Bhai Supermarket",
                    phone = "+91 98790 65432",
                    address = "Main Highway Junction",
                    customerType = "Customer",
                    balance = 12500.0 // You will get 12500
                ),
                KhataCustomerEntity(
                    id = "CUST_3",
                    name = "Gujarat Wholesale Traders",
                    phone = "+91 99090 11223",
                    address = "GIDC Phase 2",
                    customerType = "Supplier",
                    balance = -8000.0 // You have to give 8000
                ),
                KhataCustomerEntity(
                    id = "CUST_4",
                    name = "Aman Retail Point",
                    phone = "+91 94280 88990",
                    address = "Tower Chowk",
                    customerType = "Customer",
                    balance = 0.0 // Settled
                )
            )
            dao.insertAllKhataCustomers(defaultCustomers)

            // Seed sample initial transactions
            val initialTxns = listOf(
                KhataTransactionEntity(
                    id = "TXN_1",
                    customerId = "CUST_1",
                    customerName = "Vikas General Store",
                    amount = 6000.0,
                    type = "GAVE",
                    date = "2026-08-15",
                    time = "11:30 AM",
                    note = "Snacks and Beverages Stock Dispatch",
                    paymentMode = "Credit / Udhar"
                ),
                KhataTransactionEntity(
                    id = "TXN_2",
                    customerId = "CUST_1",
                    customerName = "Vikas General Store",
                    amount = 1500.0,
                    type = "GOT",
                    date = "2026-08-16",
                    time = "05:00 PM",
                    note = "Cash Received via GPay",
                    paymentMode = "UPI / Online"
                ),
                KhataTransactionEntity(
                    id = "TXN_3",
                    customerId = "CUST_2",
                    customerName = "Ramesh Bhai Supermarket",
                    amount = 12500.0,
                    type = "GAVE",
                    date = "2026-08-16",
                    time = "02:15 PM",
                    note = "Bulk Perfumes & Cigarettes Order",
                    paymentMode = "Credit / Udhar"
                ),
                KhataTransactionEntity(
                    id = "TXN_4",
                    customerId = "CUST_3",
                    customerName = "Gujarat Wholesale Traders",
                    amount = 8000.0,
                    type = "GOT",
                    date = "2026-08-14",
                    time = "10:00 AM",
                    note = "Raw Material Purchase Advance Due",
                    paymentMode = "Bank Transfer"
                )
            )
            for (t in initialTxns) {
                dao.insertOrUpdateKhataTransaction(t)
            }
        }
    }

    // Price Tracking & Price History Methods
    val allPriceHistory: Flow<List<PriceHistoryEntity>> = dao.getAllPriceHistoryFlow()

    fun getPriceHistoryForItem(itemKey: String): Flow<List<PriceHistoryEntity>> {
        return dao.getPriceHistoryForItemFlow(itemKey)
    }

    suspend fun updateItemPrice(
        item: InventoryItemEntity,
        newMrp: Double,
        newWholesale: Double,
        newCost: Double,
        updatedBy: String,
        note: String
    ) {
        val now = System.currentTimeMillis()
        dao.updateItemPrice(
            key = item.key,
            mrp = newMrp,
            wholesalePrice = newWholesale,
            costPrice = newCost,
            previousMrp = if (item.mrp > 0) item.mrp else newMrp,
            previousWholesale = if (item.wholesalePrice > 0) item.wholesalePrice else newWholesale,
            timestamp = now,
            note = note
        )

        // Log to history
        val historyRecord = PriceHistoryEntity(
            itemKey = item.key,
            itemName = item.displayName.ifBlank { item.name },
            oldMrp = item.mrp,
            newMrp = newMrp,
            oldWholesale = item.wholesalePrice,
            newWholesale = newWholesale,
            oldCost = item.costPrice,
            newCost = newCost,
            updatedBy = updatedBy,
            timestamp = now,
            note = note
        )
        dao.insertPriceHistory(historyRecord)
    }

    suspend fun deletePriceHistory(id: Long) = dao.deletePriceHistory(id)
}
