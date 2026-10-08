package com.mhss.app.mybrain.presentation.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhss.app.ui.R
import com.mhss.app.ui.components.common.GlassSurface
import com.mhss.app.ui.theme.Blue
import com.mhss.app.ui.theme.MyBrainTheme
import kotlinx.coroutines.delay

@Composable
fun SpaceCard(
    title: String,
    subtitleRes: Int,
    image: Int,
    backgroundColor: Color,
    isPulsing: Boolean = false,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(15.dp)

    // 🔹 BOOSTA FÄRGEN FÖR DARK MODE + GLAS
    val boostedColor = backgroundColor.boost()

    // 🔹 BLÅ GRADIENT (SYNLIG, DJUP, PREMIUM)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            boostedColor.copy(alpha = 0.55f),
            boostedColor.copy(alpha = 0.5f)
        )
    )

    // 🔥 Heartbeat logic (active only if isPulsing == true)
    var heartbeatPhase by remember { mutableStateOf(false) }

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
        targetValue = if (heartbeatPhase) 1.08f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "heartbeat"
    )

    GlassSurface(
        shape = shape,
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔹 IKON / AVATAR (VÄNSTER) - MED PULSING
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.30f))
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.width(30.dp))

            // 🔹 TEXT (HÖGER)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.60f),
                    maxLines = 2
                )
            }
        }
    }
}

/* ---------------------------------------------------
   FÄRGBOST (VIKTIG FÖR GLAS + DARK MODE)
---------------------------------------------------- */

fun Color.boost(): Color = copy(

    red = (red * 1.1f).coerceAtMost(1f),
    green = (green * 1.1f).coerceAtMost(1f),
    blue = (blue * 1.1f).coerceAtMost(1f)
)

/* ---------------------------------------------------
   PREVIEW
---------------------------------------------------- */

@Preview(showBackground = true)
@Composable
fun SpaceCardPreview() {
    MyBrainTheme {
        SpaceCard(
            title = "Notes",
            subtitleRes = R.string.notes_subtitle,
            image = R.drawable.notes_img,
            backgroundColor = Blue
        )
    }
}