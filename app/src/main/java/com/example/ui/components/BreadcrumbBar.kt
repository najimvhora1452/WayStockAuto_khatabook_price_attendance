package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WayStockBorder
import com.example.ui.theme.WayStockPrimary
import com.example.ui.theme.WayStockTextMain
import com.example.ui.theme.WayStockTextSec

@Composable
fun BreadcrumbBar(
    pathStack: List<String>,
    onBreadcrumbClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("breadcrumb_bar"),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pathStack.forEachIndexed { index, pathKey ->
                val isLast = index == pathStack.size - 1
                val displayName = if (index == 0 && pathKey == "root") "Home" else {
                    if (pathKey.contains(">")) pathKey.split(">").last().trim() else pathKey
                }

                if (isLast) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WayStockTextMain
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onBreadcrumbClick(index) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Separator",
                        tint = WayStockTextSec.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}
