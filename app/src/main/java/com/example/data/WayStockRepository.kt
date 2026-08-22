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
        // Clean initial state for user's fresh custom data
    }

    suspend fun getAllInventoryList(): List<InventoryItemEntity> = dao.getAllInventoryList()

    suspend fun insertOrUpdateItem(item: InventoryItemEntity) = dao.insertOrUpdateItem(item)

    suspend fun insertAllItems(items: List<InventoryItemEntity>) = dao.insertAllItems(items)

    suspend fun replaceAllInventory(items: List<InventoryItemEntity>) = dao.replaceAllInventory(items)

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
        // Clean initial state for staff members
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
        // Recalculate customer balance: GAVE (You Gave / +ve You will get), GOT (You Got / -ve Reduces balance)
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
        // Clean initial state for Khata accounts
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

    // Khata Quick Memos & Notifications
    val allKhataMemos: Flow<List<KhataMemoEntity>> = dao.getAllKhataMemosFlow()

    suspend fun insertOrUpdateKhataTransactionDirect(txn: KhataTransactionEntity) {
        dao.insertOrUpdateKhataTransaction(txn)
    }

    suspend fun insertKhataMemoDirect(memo: KhataMemoEntity) {
        dao.insertKhataMemo(memo)
    }

    suspend fun insertKhataMemo(note: String): Long {
        if (note.isNotBlank()) {
            val memo = KhataMemoEntity(note = note.trim())
            dao.insertKhataMemo(memo)
            return memo.id
        }
        return 0L
    }

    suspend fun deleteKhataMemo(id: Long) = dao.deleteKhataMemo(id)

    suspend fun clearAllKhataMemos() = dao.clearAllKhataMemos()
}
