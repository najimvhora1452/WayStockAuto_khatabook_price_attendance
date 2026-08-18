package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val key: String,
    val name: String,
    val fullPath: String,
    val rootFolder: String = "Home",
    val quantity: Int = 1,
    val unit: String = "Box",
    val userId: String = "guest"
)
