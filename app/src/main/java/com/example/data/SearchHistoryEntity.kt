package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemKey: String,
    val queryText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "guest"
)
