package com.fran.dev.potjera.android.app.game.coinbooster.presentation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.CoinBoosterPlayerFinishInfo
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.Cyan
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White

@Composable
fun CoinBoosterQueueScreen(
    finishedPlayers: List<CoinBoosterPlayerFinishInfo> = emptyList(),
    totalPlayers: Int,
    isHost: Boolean,
    onStartBoardQuestions: () -> Unit,
) {
    val allFinished = finishedPlayers.size >= totalPlayers

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.Companion.height(24.dp))

            Text(
                text = if (allFinished) "✅" else "⏳",
                fontSize = 52.sp,
                modifier = if (allFinished) Modifier.Companion else Modifier.Companion.scale(alpha)
            )

            Text(
                text = if (allFinished) "Everyone finished!" else "Waiting for others...",
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Companion.Bold,
                textAlign = TextAlign.Companion.Center
            )

            Text(
                text = "${finishedPlayers.size} / $totalPlayers players finished",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Companion.Center
            )

            // progress bar
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BgCardBorder)
            ) {
                val progress =
                    if (totalPlayers > 0) finishedPlayers.size.toFloat() / totalPlayers else 0f
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                        .background(Brush.Companion.horizontalGradient(listOf(Purple, Cyan)))
                )
            }

            // finished players list
            if (finishedPlayers.isNotEmpty()) {
                Column(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Finished",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Companion.SemiBold
                    )
                    finishedPlayers.forEach { player ->
                        Row(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(BgCard)
                                .border(
                                    1.dp,
                                    Green.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                Box(
                                    modifier = Modifier.Companion
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Green.copy(alpha = 0.2f))
                                        .border(1.dp, Green.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Companion.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Green,
                                        modifier = Modifier.Companion.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.Companion.width(12.dp))
                                Text(
                                    text = player.username,
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Companion.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                Text("⚡ ", fontSize = 12.sp)
                                Text(
                                    text = "${player.moneyWon} $",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.weight(1f))

            // start board questions button — host only, all finished
            if (isHost && allFinished) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(GradButton)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onStartBoardQuestions
                            )
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Text(
                            text = "Start Board Questions →",
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Companion.Bold
                        )
                    }
                    Text(
                        text = "All players finished — ready to continue!",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else if (!isHost && allFinished) {
                // non-host sees waiting message
                Text(
                    text = "Waiting for host to start next phase...",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Companion.Center
                )
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun CoinBoosterQueueScreenPreview() {
    CoinBoosterQueueScreen(
        finishedPlayers = listOf(
            CoinBoosterPlayerFinishInfo(playerId = 1L, "matko", moneyWon = 3500f),
            CoinBoosterPlayerFinishInfo(playerId = 2L, "bratko", moneyWon = 4000f),
        ),
        totalPlayers = 4,
        isHost = true,
        {}
    )
}
