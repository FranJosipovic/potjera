package com.fran.dev.potjera.android.app.game.playersphase.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgInput
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradAvatar
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.GradButtonDim
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.Red
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White

data class PlayersAnsweringPlayer(val playerId: Long, val name: String, val emoji: String)

// ── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun PlayersPhaseScreen(
    phaseName: String = "Team Building Phase",
    phaseSubtitle: String = "Build your lead",
    timerSeconds: Int = 102,          // 1:42
    teamMembers: List<PlayersAnsweringPlayer> = listOf(
        PlayersAnsweringPlayer(1, "Player_123", "🎮"),
        PlayersAnsweringPlayer(2, "QuizMaster", "🧠"),
        PlayersAnsweringPlayer(3, "FastAnswerer", "⚡"),
        PlayersAnsweringPlayer(4, "FastAnswerer", "⚡"),
    ),
    totalSteps: Int = 3,         // steps 1-3 done, 4 active
    activeStep: Int = 4,
    questionNumber: Int = 2,
    questionText: String = "What is the square root of 144?",
    answeringPlayer: Long? = 4,
    onBuzzIn: () -> Unit = {},
    onAnswer: () -> Unit = {}
) {
    val currentPlayerId: Long = 4

    // Pulsing animation for BUZZ IN button
    val pulse = rememberInfiniteTransition(label = "buzz")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.03f, label = "scale",
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val mm = timerSeconds / 60
    val ss = timerSeconds % 60
    val timerText = "%d:%02d".format(mm, ss)
    val timerLow = timerSeconds < 30

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header row ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Phase info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BgCard)
                        .border(1.dp, BgCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👥", fontSize = 18.sp)
                }
                Column {
                    Text(
                        phaseName,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        phaseSubtitle,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Timer chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(
                        1.5.dp,
                        if (timerLow) Red else BgCardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🕐", fontSize = 14.sp)
                    Text(
                        timerText,
                        color = if (timerLow) Red else White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ── Team Members card ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "TEAM MEMBERS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                teamMembers.forEach { member ->
                    TeamMemberItem(member, answeringPlayer)
                }
            }
        }

        // ── Progress card ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PROGRESS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⚡", fontSize = 12.sp)
                    Text(
                        "$totalSteps Steps",
                        color = Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Step pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
            ) {
                for (i in 1..totalSteps) {
                    val isCompleted = i < activeStep
                    val isActive = i == activeStep
                    when {
                        isCompleted || isActive -> null
                        else -> null
                    }
                    val bgColor = when {
                        isCompleted -> Color(0xFF3A7FFF)   // bright blue
                        isActive -> Green
                        else -> Color(0xFF2A2050)   // dim
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (i == 1) 8.dp else 0.dp,
                                    bottomStart = if (i == 1) 8.dp else 0.dp,
                                    topEnd = if (i == totalSteps) 8.dp else 0.dp,
                                    bottomEnd = if (i == totalSteps) 8.dp else 0.dp,
                                )
                            )
                            .background(bgColor)
                            .then(
                                if (isActive) Modifier.border(
                                    2.dp, White.copy(alpha = .4f), RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .border(3.dp, White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$i",
                            color = if (i >= activeStep && !isCompleted && !isActive)
                                TextMuted else White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress bar
            val progress = activeStep.toFloat() / totalSteps.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2050))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(GradButton)
                )
            }
        }

        // ── BUZZ IN  /  Answer input ──────────────────────────────────────
        if (answeringPlayer == null) {
            // nobody buzzed yet — show BUZZ IN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF6B00),
                                Color(0xFFFFB703)
                            )
                        )
                    )
                    .clickable(onClick = onBuzzIn),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✋", fontSize = 22.sp)
                    Text(
                        "BUZZ IN!",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else if (answeringPlayer == currentPlayerId) {
            // current player buzzed — show answer input
            var answer by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, Purple, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "YOUR ANSWER",
                    color = Purple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                BasicTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgInput)
                        .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Purple),
                    decorationBox = { inner ->
                        if (answer.isEmpty()) Text(
                            "Type your answer…",
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                        inner()
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GradButton)
                        .clickable(onClick = onAnswer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SUBMIT",
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            // someone else buzzed — show waiting state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GradButtonDim),
                contentAlignment = Alignment.Center
            ) {
                val buzzer = teamMembers.first { it.playerId == answeringPlayer }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(buzzer.emoji, fontSize = 20.sp)
                    Text(
                        "${buzzer.name} is answering…",
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Question card ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "QUESTION $questionNumber",
                color = Purple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                questionText,
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Team member avatar + label ────────────────────────────────────────────────
@Composable
private fun TeamMemberItem(member: PlayersAnsweringPlayer, currentAnsweringPlayerId: Long? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(GradAvatar, alpha = if (member.playerId == currentAnsweringPlayerId) 1f else 0.5f)
                .border(
                    if (member.playerId == currentAnsweringPlayerId) 5.dp else 1.dp,
                    White,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(member.emoji, fontSize = 26.sp)
        }
        Text(
            member.name,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 390, heightDp = 780)
@Composable
fun PlayersPhaseScreenPreview() {
    PlayersPhaseScreen()
}