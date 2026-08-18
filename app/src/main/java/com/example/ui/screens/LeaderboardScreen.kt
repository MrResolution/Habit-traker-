package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaderboardEntry
import com.example.data.ScoringEngine
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StreakRose
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.components.SkeletonLeaderboardCard
import com.example.ui.components.SkeletonPodiumSection

@Composable
fun LeaderboardTab(
    leaderboardEntries: List<LeaderboardEntry>,
    isLoading: Boolean = false
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    var selectedEntryForBreakdown by remember { mutableStateOf<LeaderboardEntry?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = NeonPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fair Scoring • Tap any member to view detailed score breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading) {
            item {
                SkeletonPodiumSection()
            }
            items(4) {
                SkeletonLeaderboardCard()
            }
        } else {
            // Top 3 Podium
            if (leaderboardEntries.size >= 3) {
                item {
                    PodiumSection(
                        entries = leaderboardEntries.take(3),
                        currentUserId = currentUserId,
                        onItemClick = { entry -> selectedEntryForBreakdown = entry }
                    )
                }
            }

            // Empty state
            if (leaderboardEntries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No entries yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Invite friends and start tracking habits together!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Full rankings list
            if (leaderboardEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "Full Rankings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(
                    items = leaderboardEntries,
                    key = { index, entry -> if (entry.userId.isNotEmpty()) entry.userId else index.toString() }
                ) { index, entry ->
                    LeaderboardCard(
                        rank = index + 1,
                        entry = entry,
                        isCurrentUser = entry.userId == currentUserId,
                        onClick = { selectedEntryForBreakdown = entry }
                    )
                }
            }
        }
    }

    if (selectedEntryForBreakdown != null) {
        ScoreBreakdownSheet(
            entry = selectedEntryForBreakdown!!,
            onDismiss = { selectedEntryForBreakdown = null }
        )
    }
}

@Composable
fun PodiumSection(
    entries: List<LeaderboardEntry>,
    currentUserId: String?,
    onItemClick: (LeaderboardEntry) -> Unit
) {
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
                .padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd place
            if (entries.size > 1) {
                PodiumItem(
                    rank = 2,
                    entry = entries[1],
                    height = 90.dp,
                    color = Color(0xFFC0C0C0),
                    isCurrentUser = entries[1].userId == currentUserId,
                    onClick = { onItemClick(entries[1]) }
                )
            }
            // 1st place
            PodiumItem(
                rank = 1,
                entry = entries[0],
                height = 120.dp,
                color = Color(0xFFFFD700),
                isCurrentUser = entries[0].userId == currentUserId,
                onClick = { onItemClick(entries[0]) }
            )
            // 3rd place
            if (entries.size > 2) {
                PodiumItem(
                    rank = 3,
                    entry = entries[2],
                    height = 70.dp,
                    color = Color(0xFFCD7F32),
                    isCurrentUser = entries[2].userId == currentUserId,
                    onClick = { onItemClick(entries[2]) }
                )
            }
        }
    }
}

@Composable
fun PodiumItem(
    rank: Int,
    entry: LeaderboardEntry,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    isCurrentUser: Boolean,
    onClick: () -> Unit = {}
) {
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }
    val levelInfo = ScoringEngine.calculateLevelInfo(entry.score)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentUser) Brush.linearGradient(listOf(NeonPurple, CyberTeal))
                    else Brush.linearGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.2f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.displayName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = medal,
            fontSize = 18.sp
        )

        Text(
            text = entry.displayName,
            fontSize = 12.sp,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentUser) NeonPurple else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Level badge chip
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "Lvl ${levelInfo.level}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPurple
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Score pillar
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${entry.score}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "pts",
                    fontSize = 10.sp,
                    color = Color(0xFF1A1A1A).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LeaderboardCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    onClick: () -> Unit = {}
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val rankTextColor = when (rank) {
        1 -> Color(0xFFB8860B)
        2 -> Color(0xFF757575)
        3 -> Color(0xFFA0522D)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val levelInfo = ScoringEngine.calculateLevelInfo(entry.score)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) NeonPurple.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (rank <= 3) rankColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (rank <= 3) rankTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentUser) Brush.linearGradient(listOf(NeonPurple, CyberTeal))
                        else Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Lvl ${levelInfo.level}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                    if (isCurrentUser) {
                        Box(
                            modifier = Modifier
                                .background(NeonPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = StreakRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.currentStreak}d",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completions",
                            tint = CyberTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.totalCompletions}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        if (rank <= 3) rankColor.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${entry.score}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (rank <= 3) rankTextColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "pts",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreBreakdownSheet(
    entry: LeaderboardEntry,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User avatar header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, CyberTeal))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Level chip
            val levelInfo = ScoringEngine.calculateLevelInfo(entry.score)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeonPurple.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Level ${levelInfo.level}: ${levelInfo.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total Score Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL FAIR SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${entry.score} pts",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // XP Progress bar
                    LinearProgressIndicator(
                        progress = { levelInfo.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberTeal,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${levelInfo.currentLevelXp} / ${levelInfo.nextLevelXp} XP to Level ${levelInfo.level + 1}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Score Breakdown Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Rows
            val basePts = if (entry.basePoints > 0) entry.basePoints else (entry.totalCompletions * 10)
            BreakdownItemRow(icon = Icons.Default.CheckCircle, iconTint = CyberTeal, title = "Base Completion Points", value = "+$basePts pts")
            BreakdownItemRow(icon = Icons.Default.Whatshot, iconTint = StreakRose, title = "Multi-Habit Streak Synergy", value = "+${entry.streakSynergyPoints} pts")
            BreakdownItemRow(icon = Icons.Default.Bolt, iconTint = Color(0xFFFFB300), title = "30-Day Consistency Multiplier", value = "${String.format("%.2f", entry.consistencyMultiplier)}x")
            BreakdownItemRow(icon = Icons.Default.Star, iconTint = NeonPurple, title = "Perfect Day Bonuses", value = "+${entry.perfectDayBonus} pts")
            BreakdownItemRow(icon = Icons.Default.MilitaryTech, iconTint = Color(0xFF4CAF50), title = "Milestone Achievements", value = "+${entry.milestoneBonus} pts")

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BreakdownItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iconTint)
    }
}
