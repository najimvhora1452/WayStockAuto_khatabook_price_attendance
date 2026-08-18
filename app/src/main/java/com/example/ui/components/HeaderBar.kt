package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HeaderBar(
    userName: String,
    isAdminMode: Boolean,
    cartItemCount: Int,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onAddStructureClick: () -> Unit,
    onAdminSettingsClick: () -> Unit,
    onShareAppClick: () -> Unit,
    onAdminLogoutClick: () -> Unit,
    onTriggerAdmin: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title, Animated Brand Logo & User Name with hidden admin trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 800) {
                                logoTapCount++
                                if (logoTapCount >= 4) {
                                    logoTapCount = 0
                                    onTriggerAdmin?.invoke()
                                }
                            } else {
                                logoTapCount = 1
                            }
                            lastTapTime = now
                        },
                        onLongClick = {
                            onTriggerAdmin?.invoke()
                        }
                    )
            ) {
                WayStockAnimatedLogo(
                    size = 36.dp,
                    interactive = true
                )
                Text(
                    text = "WayStock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )
                if (userName.isNotEmpty()) {
                    Text(
                        text = "($userName)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = WayStockTextSec
                    )
                }
                if (isAdminMode) {
                    Surface(
                        color = WayStockPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ADMIN",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayStockPrimary
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Search Button
                IconButton(
                    onClick = {
                        menuExpanded = false
                        onSearchClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = WayStockTextMain
                    )
                }

                // Cart / Bucket Button with Badge
                Box(
                    modifier = Modifier.testTag("cart_button_box")
                ) {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("cart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "My Bucket",
                            tint = WayStockTextMain
                        )
                    }

                    if (cartItemCount > 0) {
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.5f),
                            label = "badgeScale"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .scale(scale)
                                .size(18.dp)
                                .background(WayStockPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Admin Menu Dropdown (visible or extra options in Admin Mode)
                if (isAdminMode) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("admin_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = WayStockTextMain
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .background(Color.White)
                                .border(1.dp, Color(0xFF0F172A).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = WayStockPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add New Item / Structure", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAddStructureClick()
                                },
                                modifier = Modifier.testTag("menu_item_add")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = WayStockTextSec,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Settings & Broadcast", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAdminSettingsClick()
                                },
                                modifier = Modifier.testTag("menu_item_settings")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            tint = WayStockCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share App Link", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WayStockDark)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onShareAppClick()
                                },
                                modifier = Modifier.testTag("menu_item_share")
                            )
                            HorizontalDivider(color = WayStockBorder)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            contentDescription = null,
                                            tint = WayStockDanger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Logout Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDanger)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAdminLogoutClick()
                                },
                                modifier = Modifier.testTag("menu_item_logout")
                            )
                        }
                    }
                }
            }
        }
    }
}
