package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CartItemEntity
import com.example.data.InventoryItemEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OrderSlipBitmapGenerator {

    /**
     * Generates a unique filename matching the user pattern:
     * ex: paanmasala_140826100155.png
     * (Category sanitized lowercase + '_' + ddMMyyHHmmss)
     */
    fun generateUniqueFileName(category: String): String {
        val sanitizedCat = category.lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), "")
            .replace("[^a-z0-9_]".toRegex(), "")
            .ifEmpty { "orderslip" }
        val timeStamp = SimpleDateFormat("ddMMyyHHmmss", Locale.getDefault()).format(Date())
        return "${sanitizedCat}_$timeStamp"
    }

    /**
     * Renders a clean, sober, ultra-professional receipt bitmap image for an order slip page
     */
    fun generateReceiptBitmap(
        rootFolder: String,
        items: List<CartItemEntity>,
        inventoryItems: List<InventoryItemEntity>,
        pageNumber: Int,
        totalPages: Int,
        currentDate: String
    ): Bitmap {
        val width = 1080
        val baseHeaderHeight = 310
        val rowHeight = 72
        val footerHeight = 180
        val dynamicHeight = baseHeaderHeight + (items.size * rowHeight) + footerHeight
        val height = dynamicHeight.coerceAtLeast(800)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Pure Crisp White Background
        canvas.drawColor(Color.WHITE)

        // Subtle professional card inner canvas
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), bgPaint)

        // Premium double outer border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#1D6881")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), 20f, 20f, borderPaint)

        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(28f, 28f, (width - 28).toFloat(), (height - 28).toFloat(), 14f, 14f, innerBorderPaint)

        // Top Elegant Header Brand Bar
        val accentPaint = Paint().apply {
            color = Color.parseColor("#1D6881")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(20f, 20f, (width - 20).toFloat(), 105f, 20f, 20f, accentPaint)
        canvas.drawRect(20f, 60f, (width - 20).toFloat(), 105f, accentPaint)

        // App Branding in Header
        val brandPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("📦 WAYSTOCK INVENTORY ORDER SLIP", 50f, 68f, brandPaint)

        val pageTagPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("PAGE $pageNumber OF $totalPages", (width - 50).toFloat(), 68f, pageTagPaint)

        // Title: Category Name
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(rootFolder.uppercase(Locale.ROOT), 50f, 175f, titlePaint)

        // Subtitle: Date & Item count
        val subPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 23f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Date: $currentDate   •   Page Items: ${items.size} item(s)", 50f, 218f, subPaint)

        // Table Header Background Bar
        val tableHeaderBg = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(50f, 245f, (width - 50).toFloat(), 295f, 10f, 10f, tableHeaderBg)

        // Table Headers
        val thSrPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("SR.", 68f, 280f, thSrPaint)
        canvas.drawText("ITEM DESCRIPTION", 140f, 280f, thSrPaint)

        val thQtyPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("QUANTITY", (width - 70).toFloat(), 280f, thQtyPaint)

        // Items list
        val itemNumPaint = Paint().apply {
            color = Color.parseColor("#1D6881")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        val itemNamePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val itemQtyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 29f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val rowLinePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1.2f
        }

        var currentY = 350f

        items.forEachIndexed { index, item ->
            val parentKey = if (item.fullPath.contains(">")) {
                item.fullPath.substringBeforeLast(">")
            } else ""

            val parentEntity = inventoryItems.find { it.key == parentKey }
            val displayName = if (parentEntity?.toggleOn == true) {
                "${parentEntity.name} ${item.name}"
            } else item.name

            // Draw Index
            val numStr = "${index + 1}."
            canvas.drawText(numStr, 68f, currentY, itemNumPaint)

            // Draw Name (clean truncation if too long)
            var truncName = displayName
            if (truncName.length > 34) {
                truncName = truncName.take(32) + "..."
            }
            canvas.drawText(truncName, 140f, currentY, itemNamePaint)

            // Draw Qty & Unit
            val qtyStr = "${item.quantity} ${item.unit}"
            canvas.drawText(qtyStr, (width - 70).toFloat(), currentY, itemQtyPaint)

            // Bottom line for row
            canvas.drawLine(50f, currentY + 22f, (width - 50).toFloat(), currentY + 22f, rowLinePaint)

            currentY += rowHeight
        }

        // Bottom Footer
        val footerY = height - 85f
        val divPaint = Paint().apply {
            color = Color.parseColor("#1D6881")
            strokeWidth = 2.5f
        }
        canvas.drawLine(50f, footerY - 25f, (width - 50).toFloat(), footerY - 25f, divPaint)

        val footerBrand = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("Generated via WayStock Android Application", 50f, footerY + 15f, footerBrand)

        val footerTime = Paint().apply {
            color = Color.parseColor("#16A34A")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("OFFICIAL ORDER SLIP ✅", (width - 50).toFloat(), footerY + 15f, footerTime)

        return bitmap
    }

    /**
     * Saves bitmap to MediaStore / Pictures with the requested unique name format.
     * Returns true ONLY if download succeeded, false otherwise.
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        category: String
    ): Boolean {
        val uniqueBaseName = generateUniqueFileName(category)
        val fileName = "$uniqueBaseName.png"

        var outputStream: OutputStream? = null
        var success = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WayStock")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        success = true
                    }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val wayStockDir = File(imagesDir, "WayStock")
                if (!wayStockDir.exists()) wayStockDir.mkdirs()
                val imageFile = File(wayStockDir, fileName)
                outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                success = true
            }

            if (success) {
                Toast.makeText(context, "💾 Slip saved: $fileName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "❌ Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            success = false
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
        return success
    }

    /**
     * Shares image bitmap directly with unique filename via FileProvider
     */
    fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        category: String
    ): Boolean {
        val uniqueBaseName = generateUniqueFileName(category)
        val fileName = "$uniqueBaseName.png"
        return try {
            val cachePath = File(context.cacheDir, "images")
            if (!cachePath.exists()) cachePath.mkdirs()
            val imageFile = File(cachePath, fileName)
            val stream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "🚨 Order Slip: $category ($fileName)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Order Slip ($fileName)"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
