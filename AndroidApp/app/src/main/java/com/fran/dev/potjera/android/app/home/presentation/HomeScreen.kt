package com.fran.dev.potjera.android.app.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.domain.models.user.User
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Color tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF1A1035)
private val BgCard = Color(0xFF251848)
private val BgCardBorder = Color(0xFF3A2A6A)
private val Gold = Color(0xFFF5A623)
private val Purple = Color(0xFF9B59FC)
private val Cyan = Color(0xFF06C8C8)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFFAA9FCC)

private val GradCreate = Brush.horizontalGradient(listOf(Color(0xFFCC3DF4), Color(0xFFFF6B9D)))
private val GradJoin = Brush.horizontalGradient(listOf(Color(0xFF5B5BFF), Color(0xFF00D4FF)))
private val GradCoin = Brush.linearGradient(
    colors = listOf(Color(0xFFFFB703), Color(0xFFE07B00)),
    start = Offset(0f, 0f),
    end = Offset(500f, 300f)
)

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier,
    user: User,
    onCreateRoom: () -> Unit = {},
    onJoinRoom: () -> Unit = {},
    onLeaderboard: () -> Unit = {},
    onProfile: () -> Unit = {},
) {

    val winRate =
        if (user.gamesPlayed == 0) 0 else ((user.gamesWon.toFloat() / user.gamesPlayed.toFloat()) * 100).roundToInt()

    Box(
        modifier = modifier
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
            TopBar(
                playerName = user.username,
                rankNumber = user.rank,
                onProfile = onProfile
            )
            CoinsCard(coins = user.coins)
            GameTitle()
            StatsRow(gamesPlayed = user.gamesPlayed, wins = user.gamesWon, winRate = winRate)
            GradientButton(
                text = "Create Room",
                gradient = GradCreate,
                iconEmoji = "👥",
                onClick = onCreateRoom
            )
            GradientButton(
                text = "Join Room",
                gradient = GradJoin,
                iconEmoji = "⚡",
                onClick = onJoinRoom
            )
            LeaderboardCard(onClick = onLeaderboard)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Challenge friends • Earn coins • Climb the ranks",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(playerName: String, rankNumber: Int, onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar + name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Gold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Avatar",
                    tint = BgDeep,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(playerName, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆 ", fontSize = 12.sp)
                    Text(
                        "Rank #$rankNumber",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Profile icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BgCard)
                .noRippleClick(onProfile),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Coins card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CoinsCard(coins: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GradCoin)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 ", fontSize = 14.sp)
                Text(
                    "Your Coins",
                    color = Color(0xFFFFECAA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%,d".format(coins),
                color = White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
        // Lightning badge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000)),
            contentAlignment = Alignment.Center
        ) { Text("⚡", fontSize = 26.sp) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Game title
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameTitle() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "POTJERA",
            color = Gold,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp
        )
        Text("The Ultimate Chase Quiz", color = TextMuted, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(gamesPlayed: Int, wins: Int, winRate: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(gamesPlayed.toString(), "Games", Purple, Modifier.weight(1f))
        StatCard(wins.toString(), "Wins", Cyan, Modifier.weight(1f))
        StatCard("$winRate%", "Win Rate", Gold, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = valueColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradientButton(text: String, gradient: Brush, iconEmoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .noRippleClick(onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$iconEmoji  ", fontSize = 18.sp)
            Text(
                text,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Leaderboard card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .noRippleClick(onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF352060)),
            contentAlignment = Alignment.Center
        ) { Text("🏆", fontSize = 22.sp) }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Leaderboard", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("See top players", color = TextMuted, fontSize = 12.sp)
        }

        Icon(
            Icons.Filled.ArrowForward,
            contentDescription = "Go",
            tint = Gold,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    this.clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
    )
