package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecordEntity
import com.example.data.StaffMemberEntity
import java.io.File
import java.io.FileOutputStream

object AttendanceReportImageGenerator {

    fun generateAndShareRosterImage(
        context: Context,
        selectedDateStr: String,
        displayDateStr: String,
        allStaff: List<StaffMemberEntity>,
        attendanceList: List<AttendanceRecordEntity>
    ) {
        try {
            val width = 1200
            val height = 1650
            val presentCount = attendanceList.count { it.status == "Present" }
            val halfDayCount = attendanceList.count { it.status == "Half Day" }
            val absentCount = attendanceList.count { it.status == "Absent" }
            val leaveCount = attendanceList.count { it.status == "Paid Leave" }
            val totalStaff = allStaff.size

            // Dynamic compact row height calculated to fit maximum items cleanly inside 1200x1650 px
            val headerAreaHeight = 420f
            val footerAreaHeight = 80f
            val availableTableHeight = height - headerAreaHeight - footerAreaHeight
            val calculatedRowHeight = if (allStaff.isNotEmpty()) {
                (availableTableHeight / allStaff.size).coerceIn(40f, 62f)
            } else 60f
            val rowHeight = calculatedRowHeight

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            canvas.drawColor(android.graphics.Color.parseColor("#F8FAFC"))

            // Header Background Gradient / Card
            val headerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1E293B")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 200f, headerPaint)

            // Header Title
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("WAYSTOCK ENTERPRISE", width / 2f, 75f, titlePaint)

            val subTitlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("DAILY STAFF ATTENDANCE ROSTER", width / 2f, 115f, subTitlePaint)

            val datePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#38BDF8")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Date: $displayDateStr ($selectedDateStr)", width / 2f, 165f, datePaint)

            // Summary Stats Bar Card
            val statsCardPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            val statsShadowPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val statsRect = RectF(40f, 220f, width - 40f, 330f)
            canvas.drawRoundRect(statsRect, 16f, 16f, statsCardPaint)
            canvas.drawRoundRect(statsRect, 16f, 16f, statsShadowPaint)

            // Draw 4 Stats columns
            val colWidth = (width - 80f) / 4f
            val statCategories = listOf(
                Pair("PRESENT", "$presentCount" to "#16A34A"),
                Pair("HALF DAY", "$halfDayCount" to "#D97706"),
                Pair("ABSENT", "$absentCount" to "#DC2626"),
                Pair("LEAVE", "$leaveCount" to "#2563EB")
            )

            val statLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                color = android.graphics.Color.parseColor("#64748B")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val statValPaint = Paint().apply {
                isAntiAlias = true
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            statCategories.forEachIndexed { i, cat ->
                val centerX = 40f + (i * colWidth) + (colWidth / 2f)
                statLabelPaint.color = android.graphics.Color.parseColor("#64748B")
                canvas.drawText(cat.first, centerX, 260f, statLabelPaint)
                statValPaint.color = android.graphics.Color.parseColor(cat.second.second)
                canvas.drawText(cat.second.first, centerX, 305f, statValPaint)
            }

            // Staff Roster Table Header
            var currentY = 370f
            val tableHeadPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("AVAILABLE STAFF MEMBERS & STATUS (${allStaff.size} Total):", 40f, currentY, tableHeadPaint)
            currentY += 25f

            // Separator
            val linePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                strokeWidth = 2f
            }
            canvas.drawLine(40f, currentY, width - 40f, currentY, linePaint)
            currentY += 15f

            // Staff Row Renderer
            val namePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1E293B")
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rolePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 18f
            }
            val rowBgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
            }
            val statusBadgePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val statusTextPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val inTimePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#475569")
                textSize = 17f
                textAlign = Paint.Align.RIGHT
            }

            allStaff.forEachIndexed { index, staff ->
                val rec = attendanceList.find { it.staffId == staff.id }
                val status = rec?.status ?: "Pending"
                val inTime = rec?.inTime?.let { "In: $it" } ?: ""

                val rowRect = RectF(40f, currentY, width - 40f, currentY + 58f)
                rowBgPaint.color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(rowRect, 10f, 10f, rowBgPaint)

                // Index & Name
                canvas.drawText("${index + 1}. ${staff.name}", 56f, currentY + 36f, namePaint)
                val roleStr = if (staff.role.isNotBlank()) " • ${staff.role}" else ""
                canvas.drawText(roleStr, 56f + namePaint.measureText("${index + 1}. ${staff.name}"), currentY + 36f, rolePaint)

                // In Time
                if (inTime.isNotBlank()) {
                    canvas.drawText(inTime, width - 230f, currentY + 36f, inTimePaint)
                }

                // Status Badge
                val (badgeColor, label) = when (status) {
                    "Present" -> "#16A34A" to "PRESENT"
                    "Half Day" -> "#D97706" to "HALF DAY"
                    "Absent" -> "#DC2626" to "ABSENT"
                    "Paid Leave" -> "#2563EB" to "LEAVE"
                    else -> "#94A3B8" to "PENDING"
                }
                statusBadgePaint.color = android.graphics.Color.parseColor(badgeColor)
                val badgeRect = RectF(width - 200f, currentY + 12f, width - 56f, currentY + 46f)
                canvas.drawRoundRect(badgeRect, 8f, 8f, statusBadgePaint)
                canvas.drawText(label, badgeRect.centerX(), currentY + 36f, statusTextPaint)

                currentY += rowHeight
            }

            // Footer
            currentY += 20f
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 18f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Generated via WayStock Staff Attendance • Official Company Record", width / 2f, currentY + 30f, footerPaint)

            // Save and share
            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val imageFile = File(imagesDir, "attendance_roster_${selectedDateStr}.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            bitmap.recycle()

            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "📋 Daily Staff Attendance Roster - $displayDateStr\n• Total Staff: $totalStaff\n• Present: $presentCount | Half Day: $halfDayCount | Absent: $absentCount\n\nFull staff roster with names attached in image."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Roster Image"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
