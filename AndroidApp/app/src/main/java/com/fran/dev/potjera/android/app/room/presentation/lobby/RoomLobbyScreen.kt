package com.fran.dev.potjera.android.app.room.presentation.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Colors
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF1A1035)
private val BgCard = Color(0xFF251848)
private val BgCardBorder = Color(0xFF3A2A6A)
private val BgHunterCard = Color(0xFF3D1010)
private val BgHunterBorder = Color(0xFF6B1A1A)
private val Gold = Color(0xFFF5A623)
private val Purple = Color(0xFF9B59FC)
private val Green = Color(0xFF2ECC71)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFFAA9FCC)
private val GradButton = Brush.horizontalGradient(listOf(Color(0xFF7B2FFF), Color(0xFFCC3DF4)))
private val GradPrizeCard = Brush.linearGradient(
    colors = listOf(Color(0xFF2A1848), Color(0xFF1A1035)),
    start = Offset(0f, 0f),
    end = Offset(0f, 200f)
)

// ─────────────────────────────────────────────────────────────────────────────
// Data models for lobby
// ─────────────────────────────────────────────────────────────────────────────

data class LobbyPlayer(
    val username: String,
    val rank: Int,
    val isReady: Boolean,
    val isHost: Boolean = false,
    val avatarColor: Color = Purple
)

data class LobbyHunter(
    val username: String,
    val rankLabel: String,
    val avatarColor: Color = Color(0xFFCC3333)
)

// ─────────────────────────────────────────────────────────────────────────────
// RoomLobbyScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RoomLobbyScreen(
    userId: Long,
    roomCode: String,
    onBack: () -> Unit = {},
    onStartGame: () -> Unit = {},
) {
    val maxPlayers = 5
    val prizePool = 1000
    val entryFee = 100

    val isHost = true

    val roomLobbyViewModel = hiltViewModel<RoomLobbyViewModel>()
    val players by roomLobbyViewModel.players.collectAsState()
    val hunter by roomLobbyViewModel.hunter.collectAsState()

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
            LobbyTopBar(onBack = onBack)

            // Room code card (only for private rooms)
            RoomCodeCard(code = roomCode, onCopy = {})

            // Players section
            PlayersSection(
                players = players,
                maxPlayers = maxPlayers
            )

            // Hunter section
            HunterSection(hunter = hunter)

            // Prize pool + entry fee card
            PrizePoolCard(
                prizePool = prizePool,
                entryFee = entryFee
            )

            // Start Game button (host only)
            if (isHost) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GradButton)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "Start Game",
                            color = White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "All set! Click Start Game",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LobbyTopBar(onBack: () -> Unit) {
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
            text = "Waiting Lobby",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Room code card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomCodeCard(code: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Room Code", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = code,
                color = White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Players section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayersSection(players: List<LobbyPlayer>, maxPlayers: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Players (${players.size}/$maxPlayers)",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Player rows
        players.forEach { player ->
            PlayerRow(player = player)
        }
    }
}

@Composable
private fun PlayerRow(player: LobbyPlayer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(player.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.username,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (player.isHost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("👑", fontSize = 13.sp)
                    }
                }
                Text(
                    text = "Rank #${player.rank}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Ready badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (player.isReady) Green.copy(alpha = 0.15f) else BgCardBorder)
                .border(
                    1.dp,
                    if (player.isReady) Green.copy(alpha = 0.5f) else BgCardBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (player.isReady) "Ready" else "Waiting",
                color = if (player.isReady) Green else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hunter section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HunterSection(hunter: LobbyHunter?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 ", fontSize = 18.sp)
            Text(
                text = "The Hunter",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Hunter card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgHunterCard)
                .border(1.dp, BgHunterBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hunter != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(hunter.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😈", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = hunter.username,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hunter.rankLabel,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Text(
                    text = "Waiting for hunter...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Prize pool card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrizePoolCard(prizePool: Int, entryFee: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GradPrizeCard)
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Prize Pool", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 ", fontSize = 16.sp)
                Text(
                    text = prizePool.toString(),
                    color = Gold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Entry Fee", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$entryFee coins",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun RoomLobbyScreenPreview() {
    RoomLobbyScreen(
        userId = 1,
        roomCode = "ABCD1234",
    )
}