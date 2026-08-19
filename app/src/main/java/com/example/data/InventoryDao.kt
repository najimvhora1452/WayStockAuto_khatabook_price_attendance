package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items")
    fun getAllInventoryItemsFlow(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE parent = :parentKey")
    fun getInventoryItemsByParent(parentKey: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE key = :key LIMIT 1")
    suspend fun getItemByKey(key: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(key) LIKE '%' || LOWER(:query) || '%' LIMIT 10")
    suspend fun searchInventory(query: String): List<InventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items WHERE key = :key OR key LIKE :key || '>%'")
    suspend fun deleteItemAndChildren(key: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clearInventory()

    // Cart Queries
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsFlow(userId: String): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE `key` = :key AND userId = :userId")
    suspend fun deleteCartItem(key: String, userId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND (`key` = :key OR `key` LIKE :key || '>%' OR rootFolder = :rootFolder)")
    suspend fun deleteCartItemsByKeys(key: String, rootFolder: String, userId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: String)

    // Search History Queries
    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistoryFlow(userId: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    // User Requested / Custom Items Queries
    @Query("SELECT * FROM user_requested_items ORDER BY timestamp DESC")
    fun getAllRequestedItemsFlow(): Flow<List<UserRequestedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestedItem(item: UserRequestedItemEntity)

    @Query("DELETE FROM user_requested_items WHERE id = :id")
    suspend fun deleteRequestedItem(id: Long)

    @Query("DELETE FROM user_requested_items")
    suspend fun clearAllRequestedItems()

    // Staff Members Queries
    @Query("SELECT * FROM staff_members ORDER BY addedAt ASC")
    fun getAllStaffMembersFlow(): Flow<List<StaffMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStaffMember(staff: StaffMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStaffMembers(staffList: List<StaffMemberEntity>)

    @Query("DELETE FROM staff_members WHERE id = :staffId")
    suspend fun deleteStaffMember(staffId: String)

    // Attendance Records Queries
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDateFlow(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getAttendanceForMonthFlow(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE staffId = :staffId OR staffName = :staffName ORDER BY date DESC")
    fun getAttendanceForStaffOrNameFlow(staffId: String, staffName: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE staffId = :staffId ORDER BY date DESC")
    fun getAttendanceForStaffFlow(staffId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE staffId = :staffId AND date = :date LIMIT 1")
    suspend fun getStaffAttendanceForDate(staffId: String, date: String): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendanceRecord(id: String)

    // Khata Book / Ledger Queries
    @Query("SELECT * FROM khata_customers ORDER BY updatedAt DESC")
    fun getAllKhataCustomersFlow(): Flow<List<KhataCustomerEntity>>

    @Query("SELECT * FROM khata_customers WHERE id = :customerId LIMIT 1")
    suspend fun getKhataCustomerById(customerId: String): KhataCustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKhataCustomer(customer: KhataCustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKhataCustomers(customers: List<KhataCustomerEntity>)

    @Query("DELETE FROM khata_customers WHERE id = :customerId")
    suspend fun deleteKhataCustomer(customerId: String)

    @Query("SELECT * FROM khata_transactions ORDER BY timestamp DESC")
    fun getAllKhataTransactionsFlow(): Flow<List<KhataTransactionEntity>>

    @Query("SELECT * FROM khata_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<KhataTransactionEntity>>

    @Query("SELECT * FROM khata_transactions WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTransactionForCustomer(customerId: String): KhataTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKhataTransaction(txn: KhataTransactionEntity)

    @Query("DELETE FROM khata_transactions WHERE id = :txnId")
    suspend fun deleteKhataTransaction(txnId: String)

    @Query("DELETE FROM khata_transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: String)

    // Price Tracking & Price History Queries
    @Query("SELECT * FROM price_history ORDER BY timestamp DESC")
    fun getAllPriceHistoryFlow(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE itemKey = :itemKey ORDER BY timestamp DESC")
    fun getPriceHistoryForItemFlow(itemKey: String): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(record: PriceHistoryEntity)

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun deletePriceHistory(id: Long)

    @Query("UPDATE inventory_items SET mrp = :mrp, wholesalePrice = :wholesalePrice, costPrice = :costPrice, previousMrp = :previousMrp, previousWholesale = :previousWholesale, lastPriceUpdated = :timestamp, priceNote = :note WHERE `key` = :key")
    suspend fun updateItemPrice(
        key: String,
        mrp: Double,
        wholesalePrice: Double,
        costPrice: Double,
        previousMrp: Double,
        previousWholesale: Double,
        timestamp: Long,
        note: String
    )

    // Khata Quick Memos & Notifications Queries
    @Query("SELECT * FROM khata_memos ORDER BY timestamp DESC")
    fun getAllKhataMemosFlow(): Flow<List<KhataMemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataMemo(memo: KhataMemoEntity)

    @Query("DELETE FROM khata_memos WHERE id = :id")
    suspend fun deleteKhataMemo(id: Long)

    @Query("DELETE FROM khata_memos")
    suspend fun clearAllKhataMemos()
}
