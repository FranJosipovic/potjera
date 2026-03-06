package com.fran.dev.potjera.android.app.game.hunterphase.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.playersphase.presentation.TeamMemberItem
import com.fran.dev.potjera.android.app.game.presentation.PlayersAnsweringPlayer
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgInput
import com.fran.dev.potjera.android.app.ui.theme.GradButtonDim
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Red
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun HunterPhaseScreen(
    phaseName: String = "Hunter Phase",
    phaseSubtitle: String = "Catch them all",
    playersAnsweringPlayerList: List<PlayersAnsweringPlayer>,
    totalSteps: Int,
    hunterCorrectAnswers: Int,
    questionText: String?,
    correctAnswer: String?,
    playerAnsweredCorrectly: Boolean? = null,
    isHunter: Boolean,
    onAnswer: (answer: String) -> Unit = {}
) {
    var timerSeconds by remember { mutableIntStateOf(120) }

    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
        }
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BgCard)
                        .border(1.dp, BgCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎯", fontSize = 18.sp)
                }
                Column {
                    Text(phaseName, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(phaseSubtitle, color = TextMuted, fontSize = 12.sp)
                }
            }

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
                "PLAYERS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                playersAnsweringPlayerList.forEach { member ->
                    TeamMemberItem(member)
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
                    "HUNTER PROGRESS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎯", fontSize = 12.sp)
                    Text(
                        "$hunterCorrectAnswers / $totalSteps",
                        color = Color(0xFFFF6B00),
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
                    val isCaught = i <= hunterCorrectAnswers
                    val bgColor = if (isCaught) Color(0xFFFF6B00) else Color(0xFF3A7FFF)
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
                            .border(2.dp, White.copy(alpha = .4f), RoundedCornerShape(8.dp))
                            .border(3.dp, White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isCaught) "🎯" else "$i",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress bar
            val progress = if (totalSteps > 0) hunterCorrectAnswers.toFloat() / totalSteps else 0f
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
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF6B00), Color(0xFFFFB703))
                            )
                        )
                )
            }
        }

        // ── Answer feedback ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = playerAnsweredCorrectly != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(
                        1.dp,
                        if (playerAnsweredCorrectly == true) Green.copy(alpha = 0.4f)
                        else Red.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (playerAnsweredCorrectly == true) "Correct answer!" else "Correct answer:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = correctAnswer ?: "",
                        color = if (playerAnsweredCorrectly == true) Green else Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Answer input (hunter only) ────────────────────────────────────
        if (isHunter) {
            var answer by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, Color(0xFFFF6B00).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "YOUR ANSWER",
                    color = Color(0xFFFF6B00),
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
                    cursorBrush = SolidColor(Color(0xFFFF6B00)),
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
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF6B00), Color(0xFFFFB703))
                            )
                        )
                        .clickable(onClick = {
                            onAnswer(answer)
                            answer = ""
                        }),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GradButtonDim),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👁️", fontSize = 20.sp)
                    Text(
                        "Spectating…",
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Question card ─────────────────────────────────────────────────
        if (questionText != null) {
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
                    "QUESTION",
                    color = Color(0xFFFF6B00),
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
        }

        Spacer(Modifier.height(8.dp))
    }
}