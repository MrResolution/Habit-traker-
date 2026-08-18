package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable shimmer effect modifier for skeleton loading states.
 */
fun Modifier.shimmerEffect(
    baseColor: Color? = null,
    highlightColor: Color? = null,
    durationMillis: Int = 1200
): Modifier = composed {
    val defaultBase = baseColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val defaultHighlight = highlightColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            defaultBase,
            defaultHighlight,
            defaultBase
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

@Composable
fun SkeletonHeaderSection() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(modifier = Modifier.width(140.dp).height(24.dp))
                    SkeletonBox(modifier = Modifier.width(200.dp).height(14.dp))
                }
                SkeletonBox(
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 24.dp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 6.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(modifier = Modifier.width(90.dp).height(14.dp))
                SkeletonBox(modifier = Modifier.width(50.dp).height(14.dp))
            }
        }
    }
}

@Composable
fun SkeletonHabitCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Placeholder
            SkeletonBox(
                modifier = Modifier.size(48.dp),
                cornerRadius = 14.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Details Placeholder
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(modifier = Modifier.width(130.dp).height(18.dp))
                SkeletonBox(modifier = Modifier.width(180.dp).height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(modifier = Modifier.width(60.dp).height(18.dp), cornerRadius = 10.dp)
                    SkeletonBox(modifier = Modifier.width(50.dp).height(18.dp), cornerRadius = 10.dp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox Action Placeholder
            SkeletonBox(
                modifier = Modifier.size(44.dp),
                cornerRadius = 22.dp
            )
        }
    }
}

@Composable
fun SkeletonPodiumSection() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place Skeleton
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(48.dp), cornerRadius = 24.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(64.dp).height(90.dp), cornerRadius = 12.dp)
            }
            // 1st Place Skeleton
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(54.dp), cornerRadius = 27.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(70.dp).height(14.dp))
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(72.dp).height(120.dp), cornerRadius = 12.dp)
            }
            // 3rd Place Skeleton
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(modifier = Modifier.size(48.dp), cornerRadius = 24.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.width(64.dp).height(70.dp), cornerRadius = 12.dp)
            }
        }
    }
}

@Composable
fun SkeletonLeaderboardCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.size(36.dp), cornerRadius = 18.dp)
            Spacer(modifier = Modifier.width(14.dp))
            SkeletonBox(modifier = Modifier.size(42.dp), cornerRadius = 21.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBox(modifier = Modifier.width(110.dp).height(16.dp))
                SkeletonBox(modifier = Modifier.width(80.dp).height(12.dp))
            }
            SkeletonBox(modifier = Modifier.width(50.dp).height(32.dp), cornerRadius = 10.dp)
        }
    }
}

@Composable
fun SkeletonChartCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(modifier = Modifier.width(150.dp).height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(7) {
                    SkeletonBox(
                        modifier = Modifier
                            .width(28.dp)
                            .height((40..100).random().dp),
                        cornerRadius = 6.dp
                    )
                }
            }
        }
    }
}
