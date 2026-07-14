package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitLog
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StreakRose
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeeklyTrendChart(
    logs: List<HabitLog>,
    habitCount: Int,
    modifier: Modifier = Modifier
) {
    var animationTriggered by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    // Process last 7 days completion rate
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val weeklyRates = mutableListOf<Float>()
    val dayLabels = mutableListOf<String>()

    val tempCal = cal.clone() as Calendar
    tempCal.add(Calendar.DAY_OF_YEAR, -6)

    for (i in 0..6) {
        val dateStr = sdf.format(tempCal.time)
        val logsForDay = logs.filter { it.date == dateStr }.distinctBy { it.habitId }.size
        val rate = if (habitCount > 0) logsForDay.toFloat() / habitCount else 0f
        weeklyRates.add(rate)

        val dayName = SimpleDateFormat("E", Locale.getDefault()).format(tempCal.time)
        dayLabels.add(dayName)
        tempCal.add(Calendar.DAY_OF_YEAR, 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Weekly Performance Trend",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val surfaceColor = MaterialTheme.colorScheme.surface
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 40f
                val paddingRight = 40f
                val paddingTop = 20f
                val paddingBottom = 40f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                val xSpacing = chartWidth / 6f

                // Draw background guidelines
                val lines = 4
                for (j in 0..lines) {
                    val y = paddingTop + (chartHeight / lines) * j
                    val ratePercent = (100 - (100 / lines) * j)
                    // Draw dashed helper lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 2f
                    )
                }

                if (weeklyRates.isNotEmpty()) {
                    val path = Path()
                    val fillPath = Path()

                    val firstPointX = paddingLeft
                    val firstPointY = paddingTop + chartHeight * (1f - (weeklyRates[0] * animateProgress))

                    path.moveTo(firstPointX, firstPointY)
                    fillPath.moveTo(firstPointX, height - paddingBottom)
                    fillPath.lineTo(firstPointX, firstPointY)

                    // Draw bezier curve points
                    for (i in 1..6) {
                        val x = paddingLeft + i * xSpacing
                        val y = paddingTop + chartHeight * (1f - (weeklyRates[i] * animateProgress))

                        val prevX = paddingLeft + (i - 1) * xSpacing
                        val prevY = paddingTop + chartHeight * (1f - (weeklyRates[i - 1] * animateProgress))

                        // Control points for smooth curved line
                        path.cubicTo(
                            (prevX + x) / 2f, prevY,
                            (prevX + x) / 2f, y,
                            x, y
                        )

                        fillPath.cubicTo(
                            (prevX + x) / 2f, prevY,
                            (prevX + x) / 2f, y,
                            x, y
                        )
                    }

                    fillPath.lineTo(paddingLeft + 6 * xSpacing, height - paddingBottom)
                    fillPath.close()

                    // Draw gradient fill under the line
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.35f),
                                NeonPurple.copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Draw main trend line
                    drawPath(
                        path = path,
                        color = NeonPurple,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )

                    // Draw indicator circles on trend line
                    for (i in 0..6) {
                        val x = paddingLeft + i * xSpacing
                        val y = paddingTop + chartHeight * (1f - (weeklyRates[i] * animateProgress))

                        // Outer glowing circle
                        drawCircle(
                            color = CyberTeal,
                            radius = 12f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Overlay day names at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categories: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val totalCount = categories.values.sum()

    val colors = listOf(NeonPurple, CyberTeal, StreakRose, Color(0xFFFBBF24))
    val categoryList = categories.keys.toList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Categories Split",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val strokeWidth = 32f

                    if (totalCount == 0) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        categoryList.forEachIndexed { index, category ->
                            val count = categories[category] ?: 0
                            val sweepAngle = (count.toFloat() / totalCount) * 360f
                            val color = colors[index % colors.size]

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCount",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (totalCount == 0) {
                    Text(
                        text = "No category data",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    categoryList.forEachIndexed { index, category ->
                        val count = categories[category] ?: 0
                        val color = colors[index % colors.size]
                        val percentage = ((count.toFloat() / totalCount) * 100).toInt()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = "$category ($percentage%)",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyHeatmapGrid(
    logs: List<HabitLog>,
    modifier: Modifier = Modifier
) {
    // Generate a simple grid of last 28 days
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -27) // 4 weeks ago

    val days = mutableListOf<String>()
    for (i in 0..27) {
        days.add(sdf.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Consistency Map",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Visualize your active streak footprint over the last 4 weeks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Render 4 rows (weeks) of 7 squares
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (week in 0..3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (day in 0..6) {
                        val index = week * 7 + day
                        val dateStr = days[index]
                        val doneCount = logs.filter { it.date == dateStr }.distinctBy { it.habitId }.size

                        // Color intensity based on habits done on that day
                        val squareColor = when {
                            doneCount == 0 -> Color.White.copy(alpha = 0.05f)
                            doneCount == 1 -> CyberTeal.copy(alpha = 0.4f)
                            doneCount == 2 -> NeonPurple.copy(alpha = 0.7f)
                            else -> NeonPurple // Full brilliance
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(squareColor, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
            Box(modifier = Modifier.size(10.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(10.dp).background(CyberTeal.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(10.dp).background(NeonPurple.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(10.dp).background(NeonPurple, RoundedCornerShape(2.dp)))
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
