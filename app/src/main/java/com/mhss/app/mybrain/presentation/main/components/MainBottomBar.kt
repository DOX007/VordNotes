package com.mhss.app.mybrain.presentation.main.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MainBottomBar(
    navController: NavHostController,
    items: List<BottomNavItem>,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedIndex = items.indexOfFirst {
        currentDestination?.route == it.screen::class.qualifiedName
    }.let { if (it == -1) 0 else it }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val bubbleOffset by animateDpAsState(
            targetValue = ((selectedIndex * 80) + 10).dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "bubbleOffset"
        )

        // Animated sliding bubble
        Box(
            modifier = Modifier
                .offset(x = bubbleOffset)
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer) // ← ÄNDRAT
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(items[selectedIndex].iconSelected),
                contentDescription = stringResource(items[selectedIndex].title),
                tint = MaterialTheme.colorScheme.onSecondaryContainer, // ← ÄNDRAT
                modifier = Modifier.size(32.dp)
            )
        }

        // Main navigation bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) // ← ÄNDRAT
                .zIndex(0f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(item.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = stringResource(item.title),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // ← ÄNDRAT
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}