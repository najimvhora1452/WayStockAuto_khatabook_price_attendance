package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Customer / Party entity for Khata Book ledger
 */
@Entity(tableName = "khata_customers")
data class KhataCustomerEntity(
    @PrimaryKey
    val id: String, // e.g. "CUST_17182928"
    val name: String,
    val phone: String = "",
    val address: String = "",
    val customerType: String = "Customer", // "Customer", "Supplier", "Staff Khata"
    val balance: Double = 0.0, // Positive = You will get (Lene Baaki / Udhar), Negative = You have to give (Dene Baaki / Advance)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Individual Transaction entry (Udhar Diya / Jama Liya)
 */
@Entity(tableName = "khata_transactions")
data class KhataTransactionEntity(
    @PrimaryKey
    val id: String, // e.g. "TXN_17182928"
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val type: String, // "GAVE" (You Gave / Udhar Diya / Red) or "GOT" (You Received / Jama Kiya / Green)
    val date: String, // "YYYY-MM-DD" e.g. "2026-08-17"
    val time: String = "", // "04:30 PM"
    val note: String = "", // e.g. "Inventory items purchase", "Cash payment"
    val paymentMode: String = "Cash", // "Cash", "UPI / Online", "Bank Transfer", "Cheque"
    val billNumber: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
