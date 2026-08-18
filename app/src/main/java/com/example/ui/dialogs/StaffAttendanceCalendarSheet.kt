package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecordEntity
import com.example.data.StaffMemberEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceCalendarSheet(
    staff: StaffMemberEntity,
    attendanceRecords: List<AttendanceRecordEntity>,
    isAdminMode: Boolean,
    onDismiss: () -> Unit,
    onMarkDateAttendance: (date: String, status: String, note: String, advanceTaken: Double) -> Unit,
    onUpdateAdvance: (newAdvance: Double) -> Unit,
    onDeleteStaff: (staffId: String) -> Unit
) {
    val context = LocalContext.current
    var selectedCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayToEdit by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAdvanceEditDialog by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showSalarySlipModal by remember { mutableStateOf(false) }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val ymdFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val currentMonthDays = remember(selectedCalendarMonth.timeInMillis) {
        val cal = selectedCalendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysList = mutableListOf<Date>()
        for (i in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            daysList.add(cal.time)
        }
        daysList
    }

    val totalMonthDays = currentMonthDays.size

    // Month Stats for this staff member
    val monthPrefix = remember(selectedCalendarMonth.timeInMillis) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedCalendarMonth.time)
    }
    val staffMonthRecords = attendanceRecords.filter { it.date.startsWith(monthPrefix) }
    val presentDays = staffMonthRecords.count { it.status == "Present" }
    val halfDays = staffMonthRecords.count { it.status == "Half Day" }
    val absentDays = staffMonthRecords.count { it.status == "Absent" }
    val leaveDays = staffMonthRecords.count { it.status == "Paid Leave" }

    // Total advance taken from day-wise entries in this month (or fallback to staff.advanceBalance)
    val totalMonthDayAdvance = staffMonthRecords.sumOf { it.advanceTaken }
    val effectiveAdvanceDeduction = if (totalMonthDayAdvance > 0) totalMonthDayAdvance else staff.advanceBalance

    // Effective working days credited
    val effectiveDays = presentDays + (halfDays * 0.5) + leaveDays

    // Calculation based on Monthly vs Daily Salary
    val baseGrossSalary = if (staff.salaryType == "Monthly") {
        if (totalMonthDays > 0) {
            (staff.monthlySalary / totalMonthDays.toDouble()) * effectiveDays
        } else staff.monthlySalary
    } else {
        (presentDays * staff.dailyWage) + (halfDays * (staff.dailyWage / 2.0)) + (leaveDays * staff.dailyWage)
    }

    val finalNetPayable = (baseGrossSalary - effectiveAdvanceDeduction).coerceAtLeast(0.0)

    // Filtered Important Records for the scrollable drop section (only days with notes OR advance taken OR special non-present)
    val importantDayRecords = remember(staffMonthRecords) {
        staffMonthRecords.filter { it.note.isNotBlank() || it.advanceTaken > 0.0 || it.status == "Absent" || it.status == "Paid Leave" }
            .sortedByDescending { it.date }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(WayStockBg)
            .testTag("staff_calendar_full_screen"),
        color = WayStockBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Action Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WayStockPrimary,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = staff.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val rateLabel = if (staff.salaryType == "Monthly") "₹${staff.monthlySalary.toInt()}/mo" else "₹${staff.dailyWage.toInt()}/day"
                                Text(
                                    text = "${staff.role} • $rateLabel",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // WhatsApp Share Menu
                            IconButton(onClick = { showShareOptionsDialog = true }) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share on WhatsApp", tint = Color.White)
                            }

                            // Delete / Remove Staff (Hide for self profile)
                            if (!staff.id.startsWith("STAFF_SELF_")) {
                                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                    Icon(Icons.Outlined.PersonRemove, contentDescription = "Remove Staff", tint = Color.White.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
            ) {
                // 2. Month Selector & Compact Overview Card with "View & Generate Salary Slip" Button
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Month Nav Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val cal = selectedCalendarMonth.clone() as Calendar
                                        cal.add(Calendar.MONTH, -1)
                                        selectedCalendarMonth = cal
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = WayStockDark)
                                }

                                Text(
                                    text = monthFormatter.format(selectedCalendarMonth.time),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockDark
                                )

                                IconButton(
                                    onClick = {
                                        val cal = selectedCalendarMonth.clone() as Calendar
                                        cal.add(Calendar.MONTH, 1)
                                        selectedCalendarMonth = cal
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = WayStockDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4 Mini Metric Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MonthStatItem("Present", "$presentDays", Color(0xFF16A34A), Modifier.weight(1f))
                                MonthStatItem("Half Day", "$halfDays", Color(0xFFD97706), Modifier.weight(1f))
                                MonthStatItem("Absent", "$absentDays", Color(0xFFDC2626), Modifier.weight(1f))
                                MonthStatItem("Leave", "$leaveDays", Color(0xFF2563EB), Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Compact Net Salary Summary Bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = WayStockPrimary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WayStockPrimary.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Net Salary Payout", fontSize = 11.5.sp, color = WayStockTextSec, fontWeight = FontWeight.Medium)
                                        Text("₹${finalNetPayable.roundToInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = WayStockPrimary)
                                    }

                                    if (effectiveAdvanceDeduction > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEE2E2)
                                        ) {
                                            Text(
                                                text = "Adv: ₹${effectiveAdvanceDeduction.toInt()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WayStockDanger,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Button to View & Generate Salary Slip Popup
                            Button(
                                onClick = { showSalarySlipModal = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Salary Slip", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. Calendar Grid Title
                item {
                    Text(
                        text = "🗓️ Monthly Calendar & Past Dates",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Tap on any day to mark attendance, note or advance payment.",
                        fontSize = 11.5.sp,
                        color = WayStockTextSec,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                    )
                }

                // 4. Calendar Days Grid
                item {
                    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Weekday headers
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                daysOfWeek.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayName == "Sun") Color(0xFFEF4444) else WayStockTextSec,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Grid of dates
                            val firstDayCal = selectedCalendarMonth.clone() as Calendar
                            firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
                            val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sun

                            val totalSlots = firstDayOfWeek + currentMonthDays.size
                            val rowCount = (totalSlots + 6) / 7

                            for (rowIndex in 0 until rowCount) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val slotIndex = (rowIndex * 7) + colIndex
                                        val dayNumber = slotIndex - firstDayOfWeek + 1

                                        if (slotIndex < firstDayOfWeek || dayNumber > currentMonthDays.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        } else {
                                            val dateObj = currentMonthDays[dayNumber - 1]
                                            val ymd = ymdFormatter.format(dateObj)
                                            val record = attendanceRecords.find { it.date == ymd }
                                            val isToday = ymd == ymdFormatter.format(Date())

                                            CalendarDayCell(
                                                dayNumber = dayNumber,
                                                record = record,
                                                isToday = isToday,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedDayToEdit = ymd }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Important Activity & Advance Log (Dedicated Self-Contained Scrollable Drop Section)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Activity & Advance Log (${importantDayRecords.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WayStockDark
                                    )
                                }

                                Text(
                                    text = "Notes & Advances",
                                    fontSize = 11.sp,
                                    color = WayStockTextSec,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (importantDayRecords.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("✨", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Iss month koi Advance ya Special Notes nahi hai.",
                                            fontSize = 11.5.sp,
                                            color = WayStockTextSec,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Dedicated fixed-height scrollable list box for important notes & advances
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(importantDayRecords, key = { it.id }) { rec ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedDayToEdit = rec.date },
                                                color = Color.White,
                                                shadowElevation = 0.5.dp
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = when (rec.status) {
                                                                    "Present" -> Color(0xFFDCFCE7)
                                                                    "Half Day" -> Color(0xFFFEF3C7)
                                                                    "Absent" -> Color(0xFFFEE2E2)
                                                                    else -> Color(0xFFDBEAFE)
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = when (rec.status) {
                                                                            "Present" -> "🟢"
                                                                            "Half Day" -> "🟡"
                                                                            "Absent" -> "🔴"
                                                                            else -> "🔵"
                                                                        },
                                                                        fontSize = 9.sp
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = rec.date,
                                                                fontSize = 12.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = WayStockDark
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "(${rec.status})",
                                                                fontSize = 11.sp,
                                                                color = WayStockTextSec
                                                            )
                                                        }

                                                        if (rec.advanceTaken > 0.0) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFFEE2E2)
                                                            ) {
                                                                Text(
                                                                    text = "Advance: ₹${rec.advanceTaken.toInt()}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = WayStockDanger,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (rec.note.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "📝 ${rec.note}",
                                                            fontSize = 11.5.sp,
                                                            color = WayStockPrimary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Full Generated Salary Slip Popup with Dismiss (Cross) Button
    if (showSalarySlipModal) {
        val monthLabel = monthFormatter.format(selectedCalendarMonth.time)

        AlertDialog(
            onDismissRequest = { showSalarySlipModal = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WayStockPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salary Slip", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    }
                    IconButton(
                        onClick = { showSalarySlipModal = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Slip", tint = WayStockTextSec)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Employee", fontSize = 12.sp, color = WayStockTextSec)
                                Text(staff.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Month", fontSize = 12.sp, color = WayStockTextSec)
                                Text(monthLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Wage Type", fontSize = 12.sp, color = WayStockTextSec)
                                Text(
                                    if (staff.salaryType == "Monthly") "Monthly (₹${staff.monthlySalary.toInt()})" else "Daily (₹${staff.dailyWage.toInt()})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WayStockDark
                                )
                            }

                            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Present / Effective Days", fontSize = 12.sp, color = WayStockTextSec)
                                Text("${effectiveDays} Days (P:$presentDays, H:$halfDays, L:$leaveDays)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Earned Gross Salary", fontSize = 12.sp, color = WayStockTextSec)
                                Text("₹${baseGrossSalary.roundToInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                            }

                            if (effectiveAdvanceDeduction > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Advance / Khata Deduction", fontSize = 12.sp, color = WayStockDanger)
                                    Text("- ₹${effectiveAdvanceDeduction.toInt()}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WayStockDanger)
                                }
                            }

                            Divider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Net Payable Salary", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                Text(
                                    "₹${finalNetPayable.roundToInt()}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WayStockPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val slipText = buildString {
                            append("══════════════════════════════\n")
                            append("     🏢 *WAYSTOCK ENTERPRISE*\n")
                            append("     *MONTHLY SALARY SLIP*\n")
                            append("══════════════════════════════\n\n")
                            append("👤 *Employee:* ${staff.name}\n")
                            append("💼 *Role:* ${staff.role}\n")
                            append("📅 *Month:* $monthLabel\n")
                            append("📌 *Wage Type:* ${if (staff.salaryType == "Monthly") "Fixed Monthly" else "Daily Wage"}\n\n")
                            append("📊 *ATTENDANCE BREAKDOWN:*\n")
                            append("• Total Month Days: $totalMonthDays\n")
                            append("• Present: $presentDays Days 🟢\n")
                            append("• Half-Day: $halfDays Days (0.5 credit) 🟡\n")
                            append("• Paid Leaves: $leaveDays Days 🔵\n")
                            append("• Absent: $absentDays Days 🔴\n")
                            append("• Total Credit Days: $effectiveDays Days\n\n")
                            append("──────────────────────────────\n")
                            if (staff.salaryType == "Monthly") {
                                append("💰 *Base Salary:* ₹${staff.monthlySalary.toInt()} / mo\n")
                            } else {
                                append("💰 *Daily Rate:* ₹${staff.dailyWage.toInt()} / day\n")
                            }
                            append("📈 *Earned Gross:* ₹${baseGrossSalary.roundToInt()}\n")
                            if (effectiveAdvanceDeduction > 0) {
                                append("➖ *Advance Deduction:* ₹${effectiveAdvanceDeduction.toInt()}\n")
                            }
                            append("══════════════════════════════\n")
                            append("💵 *NET PAYABLE AMOUNT: ₹${finalNetPayable.roundToInt()}*\n")
                            append("══════════════════════════════\n\n")
                            append("Status: Verified & Approved ✅\n")
                            append("_Generated via WayStock Staff Management_")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, slipText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Salary Slip"))
                        showSalarySlipModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share on WhatsApp", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSalarySlipModal = false }) {
                    Text("Close", color = WayStockTextSec)
                }
            }
        )
    }

    // Share Options Dialog: 1. Clean Salary Slip, 2. Detailed Leaves & Half-Days Breakdown
    if (showShareOptionsDialog) {
        val monthLabel = monthFormatter.format(selectedCalendarMonth.time)

        // Find list of specific leaves, half-days, absents with dates & notes
        val halfDaysList = staffMonthRecords.filter { it.status == "Half Day" }.sortedBy { it.date }
        val absentDaysList = staffMonthRecords.filter { it.status == "Absent" }.sortedBy { it.date }
        val leaveDaysList = staffMonthRecords.filter { it.status == "Paid Leave" }.sortedBy { it.date }
        val advanceDaysList = staffMonthRecords.filter { it.advanceTaken > 0.0 }.sortedBy { it.date }

        AlertDialog(
            onDismissRequest = { showShareOptionsDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = WayStockPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share on WhatsApp", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Aap ${staff.name} ko konsa format WhatsApp par send karna chahte hain?",
                        fontSize = 12.5.sp,
                        color = WayStockTextSec
                    )

                    // Option A: Professional Pay Slip (Executive Summary)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val slipText = buildString {
                                    append("══════════════════════════════\n")
                                    append("     🏢 *WAYSTOCK ENTERPRISE*\n")
                                    append("     *MONTHLY SALARY SLIP*\n")
                                    append("══════════════════════════════\n\n")
                                    append("👤 *Employee:* ${staff.name}\n")
                                    append("💼 *Role:* ${staff.role}\n")
                                    append("📅 *Month:* $monthLabel\n")
                                    append("📌 *Wage Type:* ${if (staff.salaryType == "Monthly") "Fixed Monthly" else "Daily Wage"}\n\n")
                                    append("📊 *ATTENDANCE BREAKDOWN:*\n")
                                    append("• Total Month Days: $totalMonthDays\n")
                                    append("• Present: $presentDays Days 🟢\n")
                                    append("• Half-Day: $halfDays Days (0.5 credit) 🟡\n")
                                    append("• Paid Leaves: $leaveDays Days 🔵\n")
                                    append("• Absent: $absentDays Days 🔴\n")
                                    append("• Total Credit Days: $effectiveDays Days\n\n")
                                    append("──────────────────────────────\n")
                                    if (staff.salaryType == "Monthly") {
                                        append("💰 *Base Salary:* ₹${staff.monthlySalary.toInt()} / mo\n")
                                    } else {
                                        append("💰 *Daily Rate:* ₹${staff.dailyWage.toInt()} / day\n")
                                    }
                                    append("📈 *Earned Gross:* ₹${baseGrossSalary.roundToInt()}\n")
                                    if (effectiveAdvanceDeduction > 0) {
                                        append("➖ *Advance Deduction:* ₹${effectiveAdvanceDeduction.toInt()}\n")
                                    }
                                    append("══════════════════════════════\n")
                                    append("💵 *NET PAYABLE AMOUNT: ₹${finalNetPayable.roundToInt()}*\n")
                                    append("══════════════════════════════\n\n")
                                    append("Status: Verified & Approved ✅\n")
                                    append("_Generated via WayStock Staff Management_")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, slipText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Salary Slip"))
                                showShareOptionsDialog = false
                            },
                        color = WayStockPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WayStockPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("📄 Standard Monthly Salary Slip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                                Text("Gross salary, advance deduction & net payable amount", fontSize = 11.sp, color = WayStockTextSec)
                            }
                        }
                    }

                    // Option B: Detailed Leave / Half-Day & Advance Breakdown Sheet
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val logText = buildString {
                                    append("══════════════════════════════\n")
                                    append("     📋 *MONTHLY ATTENDANCE & ADVANCE LOG*\n")
                                    append("══════════════════════════════\n\n")
                                    append("👤 *Staff:* ${staff.name} (${staff.role})\n")
                                    append("🗓️ *Month:* $monthLabel\n\n")
                                    append("📊 *SUMMARY:* Present: $presentDays | Half-Day: $halfDays | Absent: $absentDays | Leave: $leaveDays\n\n")
                                    
                                    if (advanceDaysList.isNotEmpty()) {
                                        append("💰 *ADVANCE PAYMENTS TAKEN:*\n")
                                        advanceDaysList.forEach { rec ->
                                            val noteStr = if (rec.note.isNotBlank()) " (${rec.note})" else ""
                                            append(" • ${rec.date}: ₹${rec.advanceTaken.toInt()}$noteStr\n")
                                        }
                                        append(" Total Advance: *₹${effectiveAdvanceDeduction.toInt()}*\n\n")
                                    }

                                    if (absentDaysList.isNotEmpty()) {
                                        append("🔴 *ABSENT DATES (${absentDaysList.size} Days):*\n")
                                        absentDaysList.forEach { rec ->
                                            val noteStr = if (rec.note.isNotBlank()) " (${rec.note})" else ""
                                            append(" • ${rec.date}$noteStr\n")
                                        }
                                        append("\n")
                                    }

                                    if (halfDaysList.isNotEmpty()) {
                                        append("🟡 *HALF-DAY DATES (${halfDaysList.size} Days):*\n")
                                        halfDaysList.forEach { rec ->
                                            val noteStr = if (rec.note.isNotBlank()) " (${rec.note})" else ""
                                            append(" • ${rec.date}$noteStr\n")
                                        }
                                        append("\n")
                                    }

                                    if (leaveDaysList.isNotEmpty()) {
                                        append("🔵 *PAID LEAVES (${leaveDaysList.size} Days):*\n")
                                        leaveDaysList.forEach { rec ->
                                            val noteStr = if (rec.note.isNotBlank()) " (${rec.note})" else ""
                                            append(" • ${rec.date}$noteStr\n")
                                        }
                                        append("\n")
                                    }

                                    append("══════════════════════════════\n")
                                    append("💵 *Net Payout: ₹${finalNetPayable.roundToInt()}*\n")
                                    append("_WayStock Attendance System_")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, logText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Leaves & Advance Log"))
                                showShareOptionsDialog = false
                            },
                        color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("📅 Leaves, Half-Days & Advance Log", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                Text("Dates of leaves, notes & advance payment history", fontSize = 11.sp, color = WayStockTextSec)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareOptionsDialog = false }) {
                    Text("Close", color = WayStockTextSec)
                }
            }
        )
    }

    // Modal to Edit Advance / Khata balance
    if (showAdvanceEditDialog) {
        var tempAdvance by remember { mutableStateOf(staff.advanceBalance.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showAdvanceEditDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = WayStockDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advance / Khata Balance", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter total advance amount taken by ${staff.name} for this cycle:", fontSize = 12.5.sp, color = WayStockTextSec)
                    OutlinedTextField(
                        value = tempAdvance,
                        onValueChange = { tempAdvance = it },
                        label = { Text("Advance Balance (₹)") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, color = WayStockDanger) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = tempAdvance.toDoubleOrNull() ?: 0.0
                        onUpdateAdvance(num)
                        showAdvanceEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Advance", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceEditDialog = false }) {
                    Text("Cancel", color = WayStockTextSec)
                }
            }
        )
    }

    // Modal to Mark/Edit Attendance, Advance Payment & Notes for a specific selected date
    selectedDayToEdit?.let { dateStr ->
        val existingRec = attendanceRecords.find { it.date == dateStr }
        var currentStatus by remember(dateStr) { mutableStateOf(existingRec?.status ?: "Present") }
        var noteText by remember(dateStr) { mutableStateOf(existingRec?.note ?: "") }
        var advanceInput by remember(dateStr) {
            mutableStateOf(if ((existingRec?.advanceTaken ?: 0.0) > 0.0) existingRec!!.advanceTaken.toInt().toString() else "")
        }

        AlertDialog(
            onDismissRequest = { selectedDayToEdit = null },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = WayStockPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record: $dateStr", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Status for ${staff.name}:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = WayStockTextSec)

                    // 4 status option chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Present", "Half Day", "Absent", "Paid Leave").forEach { st ->
                            val selected = currentStatus == st
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { currentStatus = st },
                                color = if (selected) WayStockPrimary else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else WayStockDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Advance Payment input for this day
                    OutlinedTextField(
                        value = advanceInput,
                        onValueChange = { advanceInput = it },
                        label = { Text("Advance Payment on this date (₹)") },
                        placeholder = { Text("e.g. 500, 1000") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, color = WayStockDanger) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Optional Note Input (Reason for leave, late in, extra task etc)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Reason (Optional)") },
                        placeholder = { Text("e.g. Doctor appointment, Late 30 mins") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val advAmount = advanceInput.toDoubleOrNull() ?: 0.0
                        onMarkDateAttendance(dateStr, currentStatus, noteText.trim(), advAmount)
                        selectedDayToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDayToEdit = null }) {
                    Text("Cancel", color = WayStockTextSec)
                }
            }
        )
    }

    // Confirmation Dialog to Remove/Resign Staff Member
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Text("Remove Staff Member?", fontWeight = FontWeight.Bold, color = WayStockDanger)
            },
            text = {
                Text(
                    "Agar ${staff.name} job chhod kar chale gaye hain, toh unka record list se remove ho jayega. Kya aap remove karna chahte hain?",
                    fontSize = 13.sp,
                    color = WayStockDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteStaff(staff.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Yes, Remove Staff", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = WayStockTextSec)
                }
            }
        )
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    record: AttendanceRecordEntity?,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val status = record?.status
    val hasAdvance = (record?.advanceTaken ?: 0.0) > 0.0
    val hasNote = record?.note?.isNotBlank() == true

    val bgColor = when (status) {
        "Present" -> Color(0xFFDCFCE7)
        "Half Day" -> Color(0xFFFEF3C7)
        "Absent" -> Color(0xFFFEE2E2)
        "Paid Leave" -> Color(0xFFDBEAFE)
        else -> if (isToday) WayStockPrimary.copy(alpha = 0.1f) else Color(0xFFF8FAFC)
    }

    val textColor = when (status) {
        "Present" -> Color(0xFF15803D)
        "Half Day" -> Color(0xFFB45309)
        "Absent" -> Color(0xFFB91C1C)
        "Paid Leave" -> Color(0xFF1D4ED8)
        else -> if (isToday) WayStockPrimary else WayStockDark
    }

    Surface(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        color = bgColor,
        border = if (isToday) androidx.compose.foundation.BorderStroke(1.5.dp, WayStockPrimary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$dayNumber",
                    fontSize = 12.sp,
                    fontWeight = if (isToday || status != null) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )

                // Indicators row: Status dot + Advance / Note badge
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasAdvance) {
                        Text("₹", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = WayStockDanger)
                    }
                    if (hasNote) {
                        Text("•", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = WayStockPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthStatItem(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = title, fontSize = 9.5.sp, color = WayStockDark, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
