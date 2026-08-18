package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemKey: String,
    val itemName: String,
    val oldMrp: Double = 0.0,
    val newMrp: Double = 0.0,
    val oldWholesale: Double = 0.0,
    val newWholesale: Double = 0.0,
    val oldCost: Double = 0.0,
    val newCost: Double = 0.0,
    val updatedBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
