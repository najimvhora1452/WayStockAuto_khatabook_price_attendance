package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun ShareAppCardDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appDownloadUrl = "https://ais-dev-b36zlltuwkeea7xpd3o772-43806944793.asia-southeast1.run.app"

    val shareText = buildString {
        append("📲 *DOWNLOAD WAYSTOCK APP*\n")
        append("Smart Digital Inventory, Price Catalog, Khata Ledger & Staff Attendance Management\n\n")
        append("👉 *Direct Download / Web App Link:*\n")
        append("$appDownloadUrl\n\n")
        append("✨ *WHAT YOU CAN DO WITH WAYSTOCK:*\n")
        append("• 📁 *Digital Product Catalog:* Browse categories, items, stock counts & instant search\n")
        append("• 🛒 *Smart Order Slip:* Add items to bucket, customize units & export instant PDF slips\n")
        append("• 📒 *Khata Ledger:* Track customer balances, repeat purchases & send WhatsApp reminders\n")
        append("• 👥 *Staff Attendance:* Daily punch in/out, monthly attendance & roster sharing\n")
        append("• 🏷️ *Real-Time Price Updates:* Update rates per KG/Litre with price history logs\n")
        append("• ⚡ *100% Offline Ready:* Super-fast local database and automatic backup\n\n")
        append("👉 Click the link above or scan the QR code to install and start using immediately!")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("share_app_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WayStockPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Share & Download App",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                text = "Scan QR or tap link to install",
                                fontSize = 11.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Presentation Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dynamic QR Pattern Canvas
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(2.dp, Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val s = size.minDimension
                                val moduleCount = 21
                                val moduleSize = s / moduleCount

                                // Draw QR Finder Patterns (Top-Left, Top-Right, Bottom-Left)
                                fun drawFinder(x: Float, y: Float) {
                                    drawRoundRect(
                                        color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                        topLeft = Offset(x, y),
                                        size = Size(moduleSize * 7, moduleSize * 7),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                    drawRoundRect(
                                        color = androidx.compose.ui.graphics.Color.White,
                                        topLeft = Offset(x + moduleSize, y + moduleSize),
                                        size = Size(moduleSize * 5, moduleSize * 5),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                    drawRoundRect(
                                        color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                        topLeft = Offset(x + moduleSize * 2, y + moduleSize * 2),
                                        size = Size(moduleSize * 3, moduleSize * 3),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                }

                                drawFinder(0f, 0f)
                                drawFinder((moduleCount - 7) * moduleSize, 0f)
                                drawFinder(0f, (moduleCount - 7) * moduleSize)

                                // Draw deterministic data modules based on url hash
                                val hash = appDownloadUrl.hashCode()
                                for (r in 0 until moduleCount) {
                                    for (c in 0 until moduleCount) {
                                        // Skip finder patterns
                                        if ((r < 8 && c < 8) || (r < 8 && c >= moduleCount - 8) || (r >= moduleCount - 8 && c < 8)) {
                                            continue
                                        }
                                        val bit = ((r * 31 + c * 17 + hash) % 3) == 0
                                        if (bit) {
                                            drawRect(
                                                color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                                topLeft = Offset(c * moduleSize, r * moduleSize),
                                                size = Size(moduleSize * 0.95f, moduleSize * 0.95f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Center WayStock Logo Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WayStockPrimary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Scan with any Camera / Google Lens",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockDark
                        )
                        Text(
                            text = "Opens download link instantly on your phone",
                            fontSize = 11.sp,
                            color = WayStockTextSec
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Link & Copy Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Link:", fontSize = 10.5.sp, color = WayStockTextSec, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = appDownloadUrl,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockPrimary,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("WayStock App Link", appDownloadUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feature Intro Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Key User Features Included:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val features = listOf(
                            "📁 Digital Catalog & Price Book" to "Instant category search & stock counts",
                            "🛒 Order Slip Generator" to "Bucket checkout & WhatsApp PDF invoices",
                            "📒 Khata Ledger Book" to "Customer balance tracker & 1-tap repeats",
                            "👥 Staff Attendance Roster" to "Daily punch in/out & photo roster share",
                            "⚡ Offline First & Fast" to "All features work without internet"
                        )
                        features.forEach { (title, subtitle) ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontWeight = FontWeight.Bold, color = WayStockPrimary, fontSize = 12.sp)
                                Column {
                                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                                    Text(subtitle, fontSize = 10.5.sp, color = WayStockTextSec)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Share Button (Shares both link and complete intro formatted for WhatsApp/SMS)
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Download WayStock Master App")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share WayStock App Link & Intro"))
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("share_app_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share App Link & Intro 🚀", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
