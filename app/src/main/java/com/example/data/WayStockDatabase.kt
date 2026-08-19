package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InventoryItemEntity::class,
        CartItemEntity::class,
        SearchHistoryEntity::class,
        UserRequestedItemEntity::class,
        StaffMemberEntity::class,
        AttendanceRecordEntity::class,
        KhataCustomerEntity::class,
        KhataTransactionEntity::class,
        PriceHistoryEntity::class,
        KhataMemoEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class WayStockDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: WayStockDatabase? = null

        fun getDatabase(context: Context): WayStockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WayStockDatabase::class.java,
                    "waystock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
