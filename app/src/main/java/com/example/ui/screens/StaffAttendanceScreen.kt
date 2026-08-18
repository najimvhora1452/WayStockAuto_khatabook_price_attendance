package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecordEntity
import com.example.data.StaffMemberEntity
import com.example.service.AttendanceActionReceiver
import com.example.ui.WayStockViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceScreen(viewModel: WayStockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val allStaff by viewModel.allStaffMembers.collectAsState()
    val dailyAttendance by viewModel.attendanceForSelectedDate.collectAsState()
    val monthlyAttendance by viewModel.attendanceForCurrentMonth.collectAsState()

    val context = LocalContext.current
    val currentDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val displayDateStr = remember(uiState.selectedAttendanceDate) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(uiState.selectedAttendanceDate)
            SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(date ?: Date())
        } catch (_: Exception) {
            uiState.selectedAttendanceDate
        }
    }

    // Stats for the selected day
    val presentCount = dailyAttendance.count { it.status == "Present" }
    val halfDayCount = dailyAttendance.count { it.status == "Half Day" }
    val absentCount = dailyAttendance.count { it.status == "Absent" }
    val leaveCount = dailyAttendance.count { it.status == "Paid Leave" }
    val totalStaff = allStaff.size

    // Self identification
    val selfStaffMember = allStaff.find { it.name.equals(uiState.userName, ignoreCase = true) }
    val selfTodayRecord = dailyAttendance.find {
        it.staffId == (selfStaffMember?.id ?: "STAFF_SELF_${uiState.userId}") ||
        it.staffName.equals(uiState.userName, ignoreCase = true)
    }

    var showShareMasterDialog by remember { mutableStateOf(false) }
    val currentMonthTitle = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("attendance_screen_content"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Top Header Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WayStockPrimary,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Staff & Attendance",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Track daily presence, overtime & payouts",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // WhatsApp Share Summary Button
                        IconButton(
                            onClick = { showShareMasterDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .size(38.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share Report", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Date Switcher Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val cal = Calendar.getInstance()
                                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(uiState.selectedAttendanceDate) ?: Date()
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                    viewModel.setSelectedAttendanceDate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                viewModel.setSelectedAttendanceDate(currentDateStr)
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.selectedAttendanceDate == currentDateStr) "Today • $displayDateStr" else displayDateStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val cal = Calendar.getInstance()
                                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(uiState.selectedAttendanceDate) ?: Date()
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    viewModel.setSelectedAttendanceDate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Self Punch In / Punch Out Card (Available to both User & Admin)
        item {
            var isReminderScheduled by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("self_punch_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = WayStockPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("⚡", fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Quick Punch (${uiState.userName.ifBlank { "You" }})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockDark
                                )
                                val statusText = selfTodayRecord?.status ?: "Not punched today"
                                Text(
                                    text = "Status: $statusText",
                                    fontSize = 12.sp,
                                    color = when (selfTodayRecord?.status) {
                                        "Present" -> Color(0xFF16A34A)
                                        "Half Day" -> Color(0xFFD97706)
                                        "Absent" -> Color(0xFFDC2626)
                                        else -> WayStockTextSec
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (selfTodayRecord?.inTime != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "In: ${selfTodayRecord.inTime}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.punchInSelf(selfStaffMember) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Punch In", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.punchOutSelf(selfStaffMember) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Punch Out", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Row: View My Own Calendar Details & Notification Reminder Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. View My Attendance & Salary Details Button
                        OutlinedButton(
                            onClick = { viewModel.openSelfDetail() },
                            modifier = Modifier.weight(1.3f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WayStockPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WayStockPrimary.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View My Details & Calendar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // 2. Interactive Notification Test / Reminder Button
                        OutlinedButton(
                            onClick = {
                                AttendanceActionReceiver.showAttendanceNotification(
                                    context,
                                    "👋 Hello ${uiState.userName.ifBlank { "there" }}! Tap below to mark Present, Half Day or Absent instantly."
                                )
                                AttendanceActionReceiver.scheduleDailyReminders(context, true)
                                isReminderScheduled = true
                                viewModel.showAlert("🔔 Test reminder sent! Check your notification bar to tap [Present] or [Absent].", "success")
                            },
                            modifier = Modifier.weight(0.9f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isReminderScheduled) Color(0xFFEFF6FF) else Color.Transparent,
                                contentColor = WayStockCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WayStockCyan.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isReminderScheduled) "Reminders Active" else "Send Notification", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 3. Daily Stats Chips Bar & Bulk Actions
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceStatChip("Present", "$presentCount", Color(0xFF16A34A), Modifier.weight(1f))
                    AttendanceStatChip("Half Day", "$halfDayCount", Color(0xFFD97706), Modifier.weight(1f))
                    AttendanceStatChip("Absent", "$absentCount", Color(0xFFDC2626), Modifier.weight(1f))
                    AttendanceStatChip("Leave", "$leaveCount", Color(0xFF2563EB), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Mark All Present & Add Staff
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Staff Members (${allStaff.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockDark
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Mark All Present Shortcut
                        TextButton(
                            onClick = { viewModel.markAllPresentForSelectedDate() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All Present", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                        }

                        // Add Staff Button
                        Button(
                            onClick = { viewModel.openAddStaffDialog(null) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Staff Attendance Cards List
        if (allStaff.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👥", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Staff Members Added", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WayStockDark)
                        Text(
                            "Tap '+ Add Staff' to register delivery boys, warehouse helpers and staff.",
                            fontSize = 12.sp,
                            color = WayStockTextSec,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(allStaff, key = { it.id }) { staff ->
                val record = dailyAttendance.find { it.staffId == staff.id }
                StaffAttendanceCard(
                    staff = staff,
                    record = record,
                    isAdminMode = uiState.isAdminMode,
                    onCardClick = { viewModel.openStaffDetail(staff) },
                    onStatusChange = { newStatus ->
                        viewModel.markAttendance(
                            staffId = staff.id,
                            staffName = staff.name,
                            status = newStatus
                        )
                    },
                    onEditClick = { viewModel.openAddStaffDialog(staff) },
                    onDeleteClick = { viewModel.deleteStaffMember(staff.id) }
                )
            }
        }
    }

    // Professional Master Share Dialog (Daily vs Full Month Attendance Count)
    if (showShareMasterDialog) {
        AlertDialog(
            onDismissRequest = { showShareMasterDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = WayStockPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Attendance via WhatsApp", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Aap konsa attendance report WhatsApp par share karna chahte hain?",
                        fontSize = 12.5.sp,
                        color = WayStockTextSec
                    )

                    // Option 1: Monthly Attendance Count & Overview of all staff
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val monthlyReport = buildString {
                                    append("══════════════════════════════\n")
                                    append("     🏢 *WAYSTOCK ENTERPRISE*\n")
                                    append("   *STAFF MONTHLY ATTENDANCE*\n")
                                    append("══════════════════════════════\n\n")
                                    append("🗓️ *Month:* $currentMonthTitle\n")
                                    append("👥 *Total Staff Enrolled:* ${allStaff.size}\n\n")
                                    append("📊 *STAFF ATTENDANCE SUMMARY:*\n")
                                    append("──────────────────────────────\n")
                                    allStaff.forEachIndexed { index, staff ->
                                        val staffRecords = monthlyAttendance.filter { it.staffId == staff.id }
                                        val present = staffRecords.count { it.status == "Present" }
                                        val halfDay = staffRecords.count { it.status == "Half Day" }
                                        val leave = staffRecords.count { it.status == "Paid Leave" }
                                        val absent = staffRecords.count { it.status == "Absent" }
                                        val effectiveCount = present + (halfDay * 0.5) + leave

                                        append("${index + 1}. *${staff.name}* (${staff.role})\n")
                                        append("   • Total Present: *${present} Days* 🟢\n")
                                        if (halfDay > 0) append("   • Half-Days: *${halfDay} Days* 🟡\n")
                                        if (leave > 0) append("   • Paid Leaves: *${leave} Days* 🔵\n")
                                        if (absent > 0) append("   • Absent: *${absent} Days* 🔴\n")
                                        append("   ✨ *Net Working Days:* *${effectiveCount} Days*\n")
                                        append("──────────────────────────────\n")
                                    }
                                    append("\nStatus: Live Verified ✅\n")
                                    append("_Generated via WayStock Staff Management System_")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, monthlyReport)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Staff Attendance"))
                                showShareMasterDialog = false
                            },
                        color = WayStockPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WayStockPrimary.copy(alpha = 0.35f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = WayStockPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("📊 Monthly Staff Attendance Count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockPrimary)
                                Text("Sabhi staff ke iss month ke Total Present, Half-Days & Leaves count", fontSize = 11.sp, color = WayStockTextSec)
                            }
                        }
                    }

                    // Option 2: Daily Attendance Report (Selected Day)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val dailyReport = buildString {
                                    append("══════════════════════════════\n")
                                    append("     🏢 *WAYSTOCK ENTERPRISE*\n")
                                    append("    *DAILY ATTENDANCE LOG*\n")
                                    append("══════════════════════════════\n\n")
                                    append("📅 *Date:* $displayDateStr\n\n")
                                    append("📊 *DAILY STATS:*\n")
                                    append("• Total Staff: $totalStaff\n")
                                    append("• Present: $presentCount 🟢\n")
                                    append("• Half-Day: $halfDayCount 🟡\n")
                                    append("• Absent: $absentCount 🔴\n")
                                    append("• Leave: $leaveCount 🔵\n\n")
                                    append("👥 *PRESENT / ACTIVE ROSTER:*\n")
                                    append("──────────────────────────────\n")
                                    allStaff.forEachIndexed { index, s ->
                                        val rec = dailyAttendance.find { it.staffId == s.id }
                                        val st = rec?.status ?: "Pending ⏳"
                                        val inTime = if (rec?.inTime != null) " [In: ${rec.inTime}]" else ""
                                        val note = if (rec?.note?.isNotBlank() == true) " (${rec.note})" else ""
                                        val icon = when (rec?.status) {
                                            "Present" -> "🟢"
                                            "Half Day" -> "🟡"
                                            "Absent" -> "🔴"
                                            "Paid Leave" -> "🔵"
                                            else -> "⏳"
                                        }
                                        append("${index + 1}. *${s.name}* (${s.role})\n")
                                        append("   $icon Status: *$st*$inTime$note\n")
                                    }
                                    append("══════════════════════════════\n")
                                    append("_Generated via WayStock Staff App_")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, dailyReport)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Daily Attendance Log"))
                                showShareMasterDialog = false
                            },
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Today, contentDescription = null, tint = WayStockDark, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("📅 Daily Attendance Roster", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                Text("Selected tarikh ka Present/Absent list with In-Time & Notes", fontSize = 11.sp, color = WayStockTextSec)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareMasterDialog = false }) {
                    Text("Close", color = WayStockTextSec)
                }
            }
        )
    }
}

@Composable
fun AttendanceStatChip(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = title, fontSize = 10.sp, color = WayStockTextSec, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun StaffAttendanceCard(
    staff: StaffMemberEntity,
    record: AttendanceRecordEntity?,
    isAdminMode: Boolean,
    onCardClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currentStatus = record?.status ?: "Pending"
    val statusOptions = listOf("Present", "Half Day", "Absent", "Paid Leave")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("staff_card_${staff.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCardClick() }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when (currentStatus) {
                            "Present" -> Color(0xFFDCFCE7)
                            "Half Day" -> Color(0xFFFEF3C7)
                            "Absent" -> Color(0xFFFEE2E2)
                            "Paid Leave" -> Color(0xFFDBEAFE)
                            else -> Color(0xFFF1F5F9)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (currentStatus) {
                                    "Present" -> "🟢"
                                    "Half Day" -> "🟡"
                                    "Absent" -> "🔴"
                                    "Paid Leave" -> "🔵"
                                    else -> "⏳"
                                },
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = staff.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "View Calendar",
                                tint = WayStockPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = staff.role, fontSize = 11.sp, color = WayStockTextSec, fontWeight = FontWeight.Medium)
                            val salaryLabel = if (staff.salaryType == "Monthly") " • ₹${staff.monthlySalary.toInt()}/mo" else " • ₹${staff.dailyWage.toInt()}/day"
                            Text(text = salaryLabel, fontSize = 11.sp, color = WayStockPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Edit / Delete Actions (Job chhod ke gaya removal & details)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Staff", tint = WayStockTextSec, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.PersonRemove, contentDescription = "Remove Staff", tint = WayStockDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Changer Segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statusOptions.forEach { status ->
                    val isSelected = currentStatus.equals(status, ignoreCase = true)
                    val chipBgColor = when (status) {
                        "Present" -> if (isSelected) Color(0xFF16A34A) else Color(0xFFF0FDF4)
                        "Half Day" -> if (isSelected) Color(0xFFD97706) else Color(0xFFFFFBEB)
                        "Absent" -> if (isSelected) Color(0xFFDC2626) else Color(0xFFFEF2F2)
                        else -> if (isSelected) Color(0xFF2563EB) else Color(0xFFEFF6FF)
                    }
                    val textColor = if (isSelected) Color.White else when (status) {
                        "Present" -> Color(0xFF16A34A)
                        "Half Day" -> Color(0xFFD97706)
                        "Absent" -> Color(0xFFDC2626)
                        else -> Color(0xFF2563EB)
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStatusChange(status) },
                        color = chipBgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }

            if (record?.markedBy != null) {
                Text(
                    text = "Marked by: ${record.markedBy}${if (record.inTime != null) " at ${record.inTime}" else ""}",
                    fontSize = 10.sp,
                    color = WayStockTextSec.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
