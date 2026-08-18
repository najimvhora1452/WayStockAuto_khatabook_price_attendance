package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val key: String,
    val name: String,
    val displayName: String = "",
    val type: String = "item", // "folder" or "item"
    val parent: String = "root",
    val toggleOn: Boolean = false,
    val allowedUnitsCsv: String = "Box,Packet,Bunch,Kg", // Comma-separated list
    val currentUnit: String = "Box",
    val mrp: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val previousMrp: Double = 0.0,
    val previousWholesale: Double = 0.0,
    val lastPriceUpdated: Long = 0L,
    val priceNote: String = ""
)
