package com.fran.dev.potjera.android.app.room.presentation.lobby

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fran.dev.potjera.android.app.room.api.RoomPlayerDTO
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgHunterBorder
import com.fran.dev.potjera.android.app.ui.theme.BgHunterCard
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.GradPrizeCard
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White

// ─────────────────────────────────────────────────────────────────────────────
// RoomLobbyScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RoomLobbyScreen(
    roomId: String,
    onBack: () -> Unit = {},
    onStartGame: (gameSessionId: String) -> Unit = {},
) {
    val maxPlayers = 5
    val prizePool = 1000
    val entryFee = 100

    val viewModel = hiltViewModel<RoomLobbyViewModel>()
    val roomDetails by viewModel.roomDetails.collectAsState()
    val players by viewModel.players.collectAsState()
    val hunter by viewModel.hunter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val isStartingGame by viewModel.isStartingGame.collectAsState()

    // handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.StartGame -> onStartGame(event.gameSessionId)
                is GameEvent.RoomClosed -> onBack()
            }
        }
    }

    LaunchedEffect(roomId) {
        viewModel.initRoom(roomId)
    }

    // intercept back press → leave room first
    BackHandler {
        viewModel.leaveRoom(roomId)
        onBack()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = White, strokeWidth = 2.dp)
        }
    } else if (roomDetails != null) {
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
                LobbyTopBar(onBack = {
                    viewModel.leaveRoom(roomId)
                    onBack()
                })

                RoomCodeCard(code = roomDetails!!.code ?: "-")

                PlayersSection(
                    players = players,
                    maxPlayers = maxPlayers,
                    isHost = isHost,
                    onAssignHunter = { playerId -> viewModel.assignHunter(roomId, playerId) },
                    onAssignCaptain = { playerId -> viewModel.assignCaptain(roomId, playerId) }
                )

                HunterSection(hunter = hunter)

                PrizePoolCard(prizePool = prizePool, entryFee = entryFee)

                if (isHost) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GradButton)
                            .clickable(
                                enabled = !isStartingGame,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                viewModel.startGame(roomId)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isStartingGame) {
                            CircularProgressIndicator(
                                color = White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
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
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Something went wrong", color = White)
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
private fun RoomCodeCard(code: String) {
    LocalContext.current
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
                onClick = {
//                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", code))
                },
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
private fun PlayersSection(
    players: List<RoomPlayerDTO>,
    maxPlayers: Int,
    isHost: Boolean,
    onAssignHunter: (Long) -> Unit,
    onAssignCaptain: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        players.forEach { player ->
            PlayerRow(
                player = player,
                isHost = isHost,
                isCurrentHunter = player.isHunter,
                isCurrentCaptain = player.isCaptain,
                onAssignHunter = { onAssignHunter(player.playerId) },
                onAssignCaptain = { onAssignCaptain(player.playerId) }
            )
        }
    }
}

@Composable
private fun PlayerRow(
    player: RoomPlayerDTO,
    isHost: Boolean,
    isCurrentHunter: Boolean,
    isCurrentCaptain: Boolean,
    onAssignHunter: () -> Unit,
    onAssignCaptain: () -> Unit,
) {
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrentHunter -> Color.Red
                            isCurrentCaptain -> Color(0xFFFFAA00)
                            else -> Purple
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isCurrentHunter -> "😈"
                        isCurrentCaptain -> "⚔️"
                        else -> "🎮"
                    },
                    fontSize = 18.sp
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
                    if (isCurrentHunter) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎯", fontSize = 13.sp)
                    }
                    if (isCurrentCaptain) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚔️", fontSize = 13.sp)
                    }
                }
                Text(
                    text = "Rank #${player.rank}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHost) {
                // assign captain button — not available for hunters
                if (!isCurrentHunter) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCurrentCaptain) Color(0xFFFFAA00).copy(alpha = 0.2f)
                                else BgCardBorder
                            )
                            .border(
                                1.dp,
                                if (isCurrentCaptain) Color(0xFFFFAA00).copy(alpha = 0.5f)
                                else BgCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onAssignCaptain
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isCurrentCaptain) "Captain ⚔️" else "Set Captain",
                            color = if (isCurrentCaptain) Color(0xFFFFAA00) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // assign hunter button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCurrentHunter) Color.Red.copy(alpha = 0.2f)
                            else BgCardBorder
                        )
                        .border(
                            1.dp,
                            if (isCurrentHunter) Color.Red.copy(alpha = 0.5f)
                            else BgCardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onAssignHunter
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isCurrentHunter) "Hunter 🎯" else "Set Hunter",
                        color = if (isCurrentHunter) Color.Red else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ready badge
            /*Box(
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
            }*/
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hunter section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HunterSection(hunter: RoomPlayerDTO?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 ", fontSize = 18.sp)
            Text(
                text = "The Hunter",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
                        .background(Color.Red),
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
                        text = "Rank #${hunter.rank}",
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
        roomId = "room123",
    )
}