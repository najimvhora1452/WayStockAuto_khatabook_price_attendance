package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_members")
data class StaffMemberEntity(
    @PrimaryKey
    val id: String, // e.g. "STAFF_17182928"
    val name: String,
    val role: String = "Staff", // "Staff", "Delivery Boy", "Warehouse Helper", "Manager"
    val phone: String = "",
    val salaryType: String = "Monthly", // "Monthly" or "Daily"
    val monthlySalary: Double = 15000.0,
    val dailyWage: Double = 500.0,
    val advanceBalance: Double = 0.0, // Running advance/khata deduction
    val isDefaultActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey
    val id: String, // e.g. "STAFFID_2026-08-17"
    val staffId: String,
    val staffName: String,
    val date: String, // "YYYY-MM-DD" e.g. "2026-08-17"
    val status: String = "Present", // "Present", "Half Day", "Absent", "Paid Leave", "Overtime"
    val inTime: String? = null, // "09:30 AM"
    val outTime: String? = null, // "06:30 PM"
    val overtimeHours: Double = 0.0,
    val note: String = "",
    val advanceTaken: Double = 0.0, // Advance / Cash taken on this specific date
    val markedBy: String = "Self", // Name of person who marked it
    val markedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
