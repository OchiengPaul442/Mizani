package com.mizani.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.ui.theme.DeepTeal
import com.mizani.money.ui.theme.Emerald

@Composable
fun MizaniMark(
    modifier: Modifier = Modifier,
    size: Int = 56,
    rounded: Boolean = false,
) {
    val shape = if (rounded) RoundedCornerShape((size * 0.24f).dp) else RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(
                // Brand mark gradient stops at Emerald; the mark's own gold dot is the focal gold.
                Brush.linearGradient(
                    listOf(DeepTeal, Emerald),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size((size * 0.92f).dp),
        )
    }
}
