package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.data.InventoryItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object RateCardImageGenerator {

    fun generateAndShareRateCardImage(
        context: Context,
        items: List<InventoryItemEntity>
    ) {
        try {
            val width = 1200
            val height = 1650
            val actualItems = items.filter { it.type == "item" }
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date())

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)

                // Background
                canvas.drawColor(Color.parseColor("#F8FAFC"))

                // Header Card
                val headerPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#0F172A")
                }
                canvas.drawRect(0f, 0f, width.toFloat(), 180f, headerPaint)

                // Title
                val titlePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.WHITE
                    textSize = 40f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("WAYSTOCK ENTERPRISE", width / 2f, 70f, titlePaint)

                val subTitlePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#94A3B8")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("OFFICIAL WHOLESALE & RETAIL RATE CARD", width / 2f, 110f, subTitlePaint)

                val datePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#38BDF8")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Effective Date: $dateStr • Live Catalog Rates", width / 2f, 150f, datePaint)

                // Table Column Headers
                val tableHeaderY = 210f
                val tableHeaderPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#1E293B")
                }
                canvas.drawRect(40f, tableHeaderY, width - 40f, tableHeaderY + 46f, tableHeaderPaint)

                val colTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.WHITE
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("#", 60f, tableHeaderY + 30f, colTextPaint)
                canvas.drawText("ITEM DESCRIPTION & CATEGORY", 120f, tableHeaderY + 30f, colTextPaint)
                canvas.drawText("UNIT", 680f, tableHeaderY + 30f, colTextPaint)
                canvas.drawText("WHOLESALE", 800f, tableHeaderY + 30f, colTextPaint)
                canvas.drawText("RETAIL (MRP)", 1010f, tableHeaderY + 30f, colTextPaint)

                var currentY = tableHeaderY + 46f
                val footerHeight = 70f
                val availableHeight = height - currentY - footerHeight
                val calculatedRowHeight = if (actualItems.isNotEmpty()) {
                    (availableHeight / actualItems.size).coerceIn(36f, 54f)
                } else 48f
                val rowHeight = calculatedRowHeight

                val rowBgEven = Paint().apply { color = Color.WHITE }
                val rowBgOdd = Paint().apply { color = Color.parseColor("#F1F5F9") }
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1.2f
                }

                val namePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#0F172A")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val catPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#64748B")
                    textSize = 15f
                }
                val unitPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#475569")
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val wholesalePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#16A34A")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val mrpPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#2563EB")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val indexPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#94A3B8")
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                actualItems.take(35).forEachIndexed { index, item ->
                    val bgPaint = if (index % 2 == 0) rowBgEven else rowBgOdd
                    canvas.drawRect(40f, currentY, width - 40f, currentY + rowHeight, bgPaint)
                    canvas.drawLine(40f, currentY + rowHeight, width - 40f, currentY + rowHeight, borderPaint)

                    // Index
                    canvas.drawText("${index + 1}", 60f, currentY + (rowHeight * 0.65f), indexPaint)

                    // Name
                    val dName = item.displayName.ifBlank { item.name }
                    canvas.drawText(dName, 120f, currentY + (rowHeight * 0.48f), namePaint)
                    val parentFolder = if (item.parent != "root") item.parent else "General"
                    canvas.drawText("📁 $parentFolder", 120f, currentY + (rowHeight * 0.85f), catPaint)

                    // Unit
                    canvas.drawText(item.currentUnit, 680f, currentY + (rowHeight * 0.65f), unitPaint)

                    // Wholesale
                    val wsStr = if (item.wholesalePrice > 0) "₹${item.wholesalePrice}" else "N/A"
                    canvas.drawText(wsStr, 800f, currentY + (rowHeight * 0.65f), wholesalePaint)

                    // MRP
                    val mrpStr = if (item.mrp > 0) "₹${item.mrp}" else "-"
                    canvas.drawText(mrpStr, 1010f, currentY + (rowHeight * 0.65f), mrpPaint)

                    currentY += rowHeight
                }

                // Footer
                val footerPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#94A3B8")
                    textSize = 17f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Generated via WayStock Rate Card • Dimensions: 1200 x 1650 px HD", width / 2f, height - 25f, footerPaint)

                // Save to file
                val imagesDir = File(context.cacheDir, "images")
                imagesDir.mkdirs()
                val imageFile = File(imagesDir, "rate_card_${System.currentTimeMillis()}.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }

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
                        "📋 WayStock Rate Card - $dateStr\nOfficial Wholesale & Retail Prices Attached."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Rate Card Image"))
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
