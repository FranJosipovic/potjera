package com.fran.dev.potjera.android.app.game.playervhunter.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.Cyan
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.Red
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun BoardPhaseSummaryScreen(
    players: List<GameSessionPlayer>,
    isHost: Boolean,
    onStartPlayersAnsweringPhase: () -> Unit,
) {
    // Only show non-hunter players who have played their board phase
    val boardPlayers = players
        .filter { !it.isHunter && it.hasPlayedBoard }
        .sortedByDescending { it.moneyWon }

    val survivorCount = boardPlayers.count { !it.isEliminated }
    val allDone       = boardPlayers.size == players.count { !it.isHunter }

    // Stagger-reveal rows
    val visibleCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(boardPlayers) {
        boardPlayers.forEachIndexed { index, _ ->
            delay(200L + index * 300L)
            visibleCount.intValue = index + 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(text = "🏁", fontSize = 52.sp)

            Text(
                text       = "Board Phase Results",
                color      = White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )

            Text(
                text     = "$survivorCount / ${boardPlayers.size} players escaped",
                color    = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            // ── Progress bar ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BgCardBorder)
            ) {
                val progress = if (boardPlayers.isNotEmpty())
                    survivorCount.toFloat() / boardPlayers.size else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Green, Cyan)))
                )
            }

            // ── Player rows ───────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                boardPlayers.forEachIndexed { index, player ->
                    AnimatedVisibility(
                        visible = visibleCount.intValue > index,
                        enter   = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 }
                    ) {
                        BoardSummaryPlayerRow(player = player)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Host action / spectator message ───────────────────────────────
            AnimatedVisibility(
                visible = visibleCount.intValue >= boardPlayers.size,
                enter   = fadeIn(tween(400))
            ) {
                if (isHost) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GradButton)
                                .clickable(
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick           = onStartPlayersAnsweringPhase
                                )
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = "Start Players Answering →",
                                color      = White,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text     = "$survivorCount players head into the next phase",
                            color    = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Text(
                        text      = "Waiting for host to continue...",
                        color     = TextMuted,
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Player row ────────────────────────────────────────────────────────────────

@Composable
private fun BoardSummaryPlayerRow(player: GameSessionPlayer) {
    val escaped      = !player.isEliminated
    val accentColor  = if (escaped) Green else Red
    val outcomeLabel = if (escaped) "Escaped" else "Caught"
    val outcomeEmoji = if (escaped) "✅" else "❌"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Left: avatar + name + outcome ─────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (player.isHost) "🎮" else "👤",
                    fontSize = 16.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = player.playerName,
                        color      = White,
                        fontSize   = 14.sp,
                        fontWeight = if (player.isHost) FontWeight.Bold else FontWeight.Normal
                    )
                    if (player.isCaptain) {
                        Text(
                            text     = "👑",
                            fontSize = 11.sp
                        )
                    }
                    if (player.isHost) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Purple.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text     = "you",
                                color    = Purple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text     = outcomeLabel,
                    color    = accentColor,
                    fontSize = 12.sp
                )
            }
        }

        // ── Right: outcome emoji + money ──────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = outcomeEmoji, fontSize = 20.sp)
            if (escaped && player.moneyWon > 0f) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("🪙", fontSize = 11.sp)
                    Text(
                        text     = "${player.moneyWon.toInt()}",
                        color    = Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun BoardPhaseSummaryScreenPreview() {
    BoardPhaseSummaryScreen(
        players = listOf(
            GameSessionPlayer(1L, "matko",  moneyWon = 4500f, isEliminated = false, isCaptain = true,  isHunter = false, isHost = true,  hasPlayedBoard = true),
            GameSessionPlayer(2L, "Alex",   moneyWon = 3000f, isEliminated = false, isCaptain = false, isHunter = false, isHost = false, hasPlayedBoard = true),
            GameSessionPlayer(3L, "Jamie",  moneyWon = 0f,    isEliminated = true,  isCaptain = false, isHunter = false, isHost = false, hasPlayedBoard = true),
            GameSessionPlayer(4L, "Morgan", moneyWon = 0f,    isEliminated = true,  isCaptain = false, isHunter = false, isHost = false, hasPlayedBoard = true),
            GameSessionPlayer(100L, "Hunter", moneyWon = 0f,  isEliminated = false, isCaptain = false, isHunter = true,  isHost = false, hasPlayedBoard = false),
        ),
        isHost = true,
        onStartPlayersAnsweringPhase = {}
    )
}