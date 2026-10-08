package com.mhss.app.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    blurRadius: Dp = 22.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.14f),
    borderColor: Color = Color.White.copy(alpha = 0.28f),
    elevation: Dp = 10.dp,
    shadowColor: Color = Color.Black,  // 🔹 NYT: Anpassningsbar skuggfärg
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor.copy(alpha = 0.15f),  // 🔹 FÖRSTÄRKT
                spotColor = shadowColor.copy(alpha = 0.30f)  // 🔹 FÖRSTÄRKT
            )
            .clip(shape)
    ) {
        // 🔹 1. BAKGRUNDSGLAS (blur + tint)
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .blur(blurRadius)
                .background(backgroundColor)
                .border(3.dp, borderColor, shape)  // 🔹 Border reducerad från 5dp
        )

        // 🔹 2. INNEHÅLL (aldrig suddigt)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            content()
        }
    }
}