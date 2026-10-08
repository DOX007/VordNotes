package com.mhss.app.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mhss.app.ui.theme.Blue
import com.mhss.app.ui.theme.LightPurple

fun DrawScope.drawAiGradientRadials(
    background: Color,
    backgroundAlpha: Float = 0.75f,
    radius: Float = size.maxDimension * 0.8f,
    baseColor: Color = Blue // 🔸 tillåter variation per kort
) {
    drawRect(background, Offset.Zero, size)

    drawGradientRadial(
        color = background
            .copy(alpha = backgroundAlpha)
            .compositeOver(baseColor),
        center = Offset(0f, size.height * 0.9f),
        radius = radius
    )

    drawGradientRadial(
        color = background
            .copy(alpha = backgroundAlpha)
            .compositeOver(Color.Black), // 🔁 ersatt orange med svart
        center = Offset(size.width * 1.1f, size.height),
        radius = radius
    )

    drawGradientRadial(
        color = background
            .copy(alpha = backgroundAlpha)
            .compositeOver(LightPurple),
        center = Offset(size.width * 1.1f, size.height * .1f),
        radius = radius
    )
}

fun DrawScope.drawGradientRadial(
    color: Color,
    center: Offset,
    radius: Float = size.maxDimension * 0.75f
) = drawRect(
    brush = Brush.radialGradient(
        colors = listOf(
            color,
            Color.Transparent
        ),
        center = center,
        radius = radius
    )
)

