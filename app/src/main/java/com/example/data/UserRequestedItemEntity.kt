package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_requested_items")
data class UserRequestedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val unit: String = "Box",
    val requestedBy: String = "User",
    val timestamp: Long = System.currentTimeMillis()
)
