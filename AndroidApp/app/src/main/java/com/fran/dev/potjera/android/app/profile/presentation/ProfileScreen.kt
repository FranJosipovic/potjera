package com.fran.dev.potjera.android.app.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.domain.models.user.User

// ─────────────────────────────────────────────────────────────────────────────
// Color tokens (shared with HomeScreen)
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF1A1035)
private val BgCard = Color(0xFF251848)
private val BgCardBorder = Color(0xFF3A2A6A)
private val Gold = Color(0xFFF5A623)
private val Purple = Color(0xFF9B59FC)
private val Cyan = Color(0xFF06C8C8)
private val Green = Color(0xFF2ECC71)
private val Red = Color(0xFFE74C3C)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFFAA9FCC)

private val GradAvatar = Brush.linearGradient(
    colors = listOf(Color(0xFFFFB703), Color(0xFFE07B00)),
    start = Offset(0f, 0f),
    end = Offset(200f, 200f)
)
private val GradAchievementUnlocked = Brush.linearGradient(
    colors = listOf(Color(0xFF6B3A00), Color(0xFF4A2800)),
    start = Offset(0f, 0f),
    end = Offset(0f, 200f)
)
private val GradAchievementLocked = Brush.linearGradient(
    colors = listOf(Color(0xFF2A1F45), Color(0xFF1E1535)),
    start = Offset(0f, 0f),
    end = Offset(0f, 200f)
)

// ─────────────────────────────────────────────────────────────────────────────
// ProfileScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    user: User,
    onBack: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            ProfileTopBar(onBack = onBack)

            // Hero card
            PlayerHeroCard(
                playerName = user.username,
                level = 12,
                rank = user.rank,
                joinedDate = "Joined January 2026",
                currentXp = user.xp,
                maxXp = 3000
            )

            // Stats 2×2 grid
            StatsGrid()

            // Statistics section
            StatisticsSection()

            // Achievements section
            AchievementsSection()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Profile",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerHeroCard(
    playerName: String,
    level: Int,
    rank: Int,
    joinedDate: String,
    currentXp: Int,
    maxXp: Int,
) {
    val progress = currentXp.toFloat() / maxXp.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar + name row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GradAvatar),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    tint = BgDeep,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = playerName,
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                // Level + Rank badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(text = "Level $level", color = Gold)
                    Badge(text = "Rank #$rank", color = Purple)
                }
                // Joined date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(joinedDate, color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        // XP progress
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Level Progress", color = TextMuted, fontSize = 13.sp)
                Text("$currentXp / $maxXp XP", color = TextMuted, fontSize = 13.sp)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Purple,
                trackColor = BgCardBorder,
            )
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats 2×2 grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard2(
                icon = Icons.Filled.Star,
                iconTint = Gold,
                label = "Total Wins",
                value = "24",
                valueColor = White,
                subLabel = "63% win rate",
                subColor = Green,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                icon = Icons.Filled.AddCircle,
                iconTint = Cyan,
                label = "Total Coins",
                value = "12.4K",
                valueColor = Gold,
                subLabel = "Available balance",
                subColor = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard2(
                icon = Icons.Filled.Done,
                iconTint = Green,
                label = "Win Streak",
                value = "5",
                valueColor = White,
                subLabel = "Best: 12",
                subColor = TextMuted,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                icon = Icons.Filled.AccountCircle,
                iconTint = Purple,
                label = "Games Played",
                value = "38",
                valueColor = White,
                subLabel = "14 losses",
                subColor = Red,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard2(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    subLabel: String,
    subColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextMuted, fontSize = 12.sp)
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(value, color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(subLabel, color = subColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Statistics section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatisticsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(icon = Icons.AutoMirrored.Filled.ExitToApp, title = "Statistics")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTileCard(
                    icon = Icons.Filled.Build,
                    iconTint = Purple,
                    value = "87%",
                    label = "Correct Answers",
                    modifier = Modifier.weight(1f)
                )
                StatTileCard(
                    icon = Icons.Filled.Done,
                    iconTint = Cyan,
                    value = "4.2s",
                    label = "Avg. Answer Time",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTileCard(
                    icon = Icons.Filled.Done,
                    iconTint = Gold,
                    value = "24.5K",
                    label = "Coins Earned",
                    modifier = Modifier.weight(1f)
                )
                StatTileCard(
                    icon = Icons.Filled.Done,
                    iconTint = Purple,
                    value = "542",
                    label = "Questions Answered",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatTileCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(value, color = White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Achievements section
// ─────────────────────────────────────────────────────────────────────────────

private data class Achievement(
    val emoji: String,
    val label: String,
    val unlocked: Boolean,
    val emojiColor: Color = Color.Unspecified
)

private val achievements = listOf(
    Achievement("🏆", "First Win", unlocked = true),
    Achievement("⭐", "10 Wins", unlocked = true),
    Achievement("⚡", "Speed Demon", unlocked = true),
    Achievement("💰", "Coin Collector", unlocked = true),
    Achievement("🎯", "50 Wins", unlocked = false),
    //Achievement("💎", "Perfect Game", unlocked = false),
)

@Composable
private fun AchievementsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(icon = Icons.Filled.Done, title = "Achievements")

        val rows = achievements.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { achievement ->
                    AchievementCard(achievement = achievement, modifier = Modifier.weight(1f))
                }
                // Fill remaining slots if row is not full
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val bgGradient = if (achievement.unlocked) GradAchievementUnlocked else GradAchievementLocked
    val borderColor = if (achievement.unlocked) Color(0xFF8B5E00) else BgCardBorder
    val labelColor = if (achievement.unlocked) Gold else TextMuted

    Column(
        modifier = modifier
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(14.dp))
            .background(bgGradient)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = achievement.emoji,
            fontSize = 32.sp,
            color = if (achievement.unlocked) Color.Unspecified else Color(0xFF555555)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = achievement.label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}