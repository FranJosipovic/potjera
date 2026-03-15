package com.fran.dev.potjera.android.app.game.gamefinished.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.GameFinishPlayerResult
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgGold
import com.fran.dev.potjera.android.app.ui.theme.BgGoldBorder
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White

@Composable
fun GameFinishedScreen(
    results: List<GameFinishPlayerResult> = emptyList(),
    myPlayerId: Long = 0L,
    onNavigateHome: () -> Unit = {},
) {
    val myResult = results.find { it.playerId == myPlayerId }
    val coinsEarned = (myResult?.correctAnswers ?: 0) * 500

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.Companion.height(16.dp))

            Text("🏆", fontSize = 56.sp)

            Text(
                text = "Game Finished!",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Companion.ExtraBold
            )

            // coins earned card
            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgGold)
                    .border(
                        1.5.dp,
                        BgGoldBorder,
                        androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Coins Earned", color = Gold.copy(alpha = 0.7f), fontSize = 14.sp)
                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                    Text("🪙 ", fontSize = 28.sp)
                    Text(
                        text = coinsEarned.toString(),
                        color = Gold,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Companion.ExtraBold
                    )
                }
                Text(
                    text = "${myResult?.correctAnswers ?: 0} correct answers × 500",
                    color = Gold.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            // results list
            if (results.isNotEmpty()) {
                Column(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Leaderboard",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Companion.SemiBold
                    )

                    results.sortedByDescending { it.correctAnswers }
                        .forEachIndexed { index, result ->
                            val isMe = result.playerId == myPlayerId
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "${index + 1}."
                            }

                            Row(
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .background(
                                        if (isMe) Purple.copy(alpha = 0.2f) else BgCard
                                    )
                                    .border(
                                        1.dp,
                                        if (isMe) Purple.copy(alpha = 0.6f) else BgCardBorder,
                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Companion.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                    Text(medal, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.Companion.width(10.dp))
                                    Text(
                                        text = "Player ${result.playerId}" + if (isMe) " (You)" else "",
                                        color = if (isMe) Purple else White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isMe) FontWeight.Companion.Bold else FontWeight.Companion.Normal
                                    )
                                }
                                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                    Text("🪙 ", fontSize = 12.sp)
                                    Text(
                                        text = "${result.correctAnswers * 500}",
                                        color = Gold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Companion.Bold
                                    )
                                }
                            }
                        }
                }
            }

            Spacer(modifier = Modifier.Companion.weight(1f))

            // Home button
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(GradButton)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onNavigateHome
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = "🏠  Back to Home",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Companion.Bold
                )
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun GameFinishedScreenPreview() {
    GameFinishedScreen(
        results = listOf(
            GameFinishPlayerResult(playerId = 1L, correctAnswers = 8),
            GameFinishPlayerResult(playerId = 2L, correctAnswers = 6),
            GameFinishPlayerResult(playerId = 3L, correctAnswers = 5),
            GameFinishPlayerResult(playerId = 4L, correctAnswers = 3),
        ),
        myPlayerId = 1L,
        onNavigateHome = {}
    )
}