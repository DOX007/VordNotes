package com.mhss.app.mybrain.presentation.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhss.app.ui.components.common.GlassSurface
import com.mhss.app.ui.R
import com.mhss.app.ui.theme.Blue
import com.mhss.app.ui.theme.MyBrainTheme
import kotlinx.coroutines.delay
import sv.lib.squircleshape.SquircleShape
import androidx.compose.ui.unit.Dp

@Composable
fun SpaceCardGrid(
    title: String,
    image: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    onClick: () -> Unit = {},
    offsetY: Dp = 0.dp
) {
    val shape = SquircleShape(
        64.dp,
        cornerSmoothing = 0.8f
    )

    // 🔥 Heartbeat logic (active only if isPulsing == true)
    var heartbeatPhase by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPulsing) {
        if (isPulsing) {
            while (true) {
                heartbeatPhase = true
                delay(120)
                heartbeatPhase = false
                delay(120)
                heartbeatPhase = true
                delay(120)
                heartbeatPhase = false
                delay(2500)
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            heartbeatPhase -> 1.05f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = modifier
            .padding(13.dp)
            .offset(y = offsetY)
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        backgroundColor = backgroundColor.copy(alpha = 0.1f),
        borderColor = Color.White.copy(alpha = 0.40f),
        elevation = 0.dp,  // 🔹 FÖRSTÄRKT SKUGGA (från 10dp)
        shadowColor = backgroundColor  // 🔹 NYT: Färgad skugga
    ) {
        // 🔹 HELT ENKELT: Text + Image, ingen inner container
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.50f),
                            backgroundColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Image(
                painter = painterResource(id = image),
                contentDescription = title,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.End)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpaceCardGridPreview() {
    MyBrainTheme {
        SpaceCardGrid(
            title = "Notes",
            image = R.drawable.notes_img,
            backgroundColor = Blue
        )
    }
}