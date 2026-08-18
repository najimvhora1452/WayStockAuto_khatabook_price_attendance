package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun WayStockLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Image(
        painter = painterResource(id = R.drawable.app_icon),
        contentDescription = "WayStock Logo",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

// Backward compatibility alias
@Composable
fun WayStockAnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    interactive: Boolean = false
) {
    WayStockLogo(modifier = modifier, size = size)
}
