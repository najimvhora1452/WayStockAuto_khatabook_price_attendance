package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InventoryItemEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShareRateCardDialog(
    items: List<InventoryItemEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val actualItems = remember(items) { items.filter { it.type == "item" } }

    val formattedRateCard = remember(actualItems) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("📋 *WAYSTOCK WHOLESALE & RETAIL RATE LIST*\n")
        sb.append("📅 Date: $dateStr\n")
        sb.append("────────────────────────\n\n")

        val grouped = actualItems.groupBy { it.parent }
        for ((cat, catItems) in grouped) {
            sb.append("📁 *${cat.uppercase()}*\n")
            for (it in catItems) {
                val name = it.displayName.ifBlank { it.name }
                val wholesale = if (it.wholesalePrice > 0) "₹${it.wholesalePrice}" else "Call for rate"
                val mrp = if (it.mrp > 0) "₹${it.mrp}" else "-"
                sb.append("• $name (${it.currentUnit}): Wholesale: $wholesale | MRP: $mrp\n")
            }
            sb.append("\n")
        }
        sb.append("────────────────────────\n")
        sb.append("📦 Order via WayStock Inventory App\n")
        sb.append("⚡ Rates subject to market revision.")
        sb.toString()
    }

    var copiedSnackbar by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("share_rate_card_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Share Wholesale Rate List",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockDark
                            )
                            Text(
                                "${actualItems.size} items ready to share",
                                fontSize = 12.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WayStockTextSec)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rate Card Preview Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WayStockBorder),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = formattedRateCard,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            color = WayStockDark,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.ui.util.RateCardImageGenerator.generateAndShareRateCardImage(context, actualItems)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("share_image_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Rate Poster (1200×1650 HD)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("WayStock Rates", formattedRateCard)
                                clipboard.setPrimaryClip(clip)
                                copiedSnackbar = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (copiedSnackbar) "Copied! ✅" else "Copy Text", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, formattedRateCard)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Rate Card via")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("share_whatsapp_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}
