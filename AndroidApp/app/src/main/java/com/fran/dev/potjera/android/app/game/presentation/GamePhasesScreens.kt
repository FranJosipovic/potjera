package com.fran.dev.potjera.android.app.game.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.fran.dev.potjera.android.app.game.models.CoinBoosterPlayerFinishInfo
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.game.models.GameFinishPlayerResult
import com.fran.dev.potjera.android.app.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// StartingScreen — countdown overlay on lobby
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StartingScreen(onCountdownFinished: () -> Unit = {}) {
    var count by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count--
        }
        onCountdownFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Starting in...",
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val scale by animateFloatAsState(
                targetValue = if (count > 0) 1f else 0.5f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "countdown_scale"
            )

            Text(
                text = if (count > 0) count.toString() else "GO!",
                color = Gold,
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.scale(scale)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CoinBoosterScreen — main quiz screen
// ─────────────────────────────────────────────────────────────────────────────

enum class AnswerState { IDLE, CORRECT, INCORRECT }

@Composable
fun CoinBoosterScreen(
    username: String = "Player_123",
    question: CoinBoosterQuestion?,
    questionIndex: Int,
    totalQuestions: Int,
    correctAnswers: Int,
    coinsBuilt: Int,
    timeLeft: Int,
    onSubmit: (String) -> Boolean,
    goToNextQuestion: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var answerState by remember { mutableStateOf(AnswerState.IDLE) }
    var correctAnswerText by remember { mutableStateOf("") }

    // reset state when question changes
    LaunchedEffect(questionIndex) {
        inputText = ""
        answerState = AnswerState.IDLE
        correctAnswerText = ""
    }

    // auto-advance after showing result
    LaunchedEffect(answerState) {
        if (answerState != AnswerState.IDLE) {
            delay(2500)
            goToNextQuestion()
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Purple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎮", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = username,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Time Left", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "${timeLeft}s",
                        color = if (timeLeft <= 10) Red else White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // ── Stats row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚡", fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$correctAnswers/$totalQuestions",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Correct", color = TextMuted, fontSize = 12.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgGold)
                        .border(1.dp, BgGoldBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🪙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = coinsBuilt.toString(),
                        color = Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Coins Built", color = Gold.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            // ── Progress bar ─────────────────────────────────────────────────
            val progress = if (totalQuestions > 0) questionIndex.toFloat() / totalQuestions else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BgCardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Purple)
                )
            }

            // ── Question label ───────────────────────────────────────────────
            Text(
                text = "Question ${questionIndex + 1} of $totalQuestions",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Question card ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = question?.question ?: "Loading...",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            // ── Answer input ─────────────────────────────────────────────────
            val inputBorderColor = when (answerState) {
                AnswerState.CORRECT -> Green
                AnswerState.INCORRECT -> Red
                AnswerState.IDLE -> BgCardBorder
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgInput)
                    .border(1.5.dp, inputBorderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (inputText.isEmpty() && answerState == AnswerState.IDLE) {
                    Text(
                        text = "Type your answer...",
                        color = TextMuted.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (answerState == AnswerState.IDLE) inputText = it },
                        textStyle = TextStyle(color = White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Purple),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (answerState != AnswerState.IDLE) {
                        Icon(
                            imageVector = if (answerState == AnswerState.CORRECT)
                                Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (answerState == AnswerState.CORRECT) Green else Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Answer reveal (always shown after submit) ─────────────────────
            AnimatedVisibility(
                visible = answerState != AnswerState.IDLE,
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
                            if (answerState == AnswerState.CORRECT) Green.copy(alpha = 0.4f)
                            else Red.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (answerState == AnswerState.CORRECT)
                                "Your answer was correct:" else "Correct answer:",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = correctAnswerText,
                            color = if (answerState == AnswerState.CORRECT) Green else Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Submit button ─────────────────────────────────────────────────
            val canSubmit = answerState == AnswerState.IDLE && inputText.isNotBlank()
            val canSkip = answerState == AnswerState.IDLE

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit) GradButton else GradButtonDim)
                    .clickable(
                        enabled = canSubmit,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val correct = onSubmit(inputText)
                        answerState = if (correct) AnswerState.CORRECT else AnswerState.INCORRECT
                        correctAnswerText = question?.answer ?: ""
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✈️  ", fontSize = 15.sp)
                    Text(
                        text = "Submit Answer",
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Skip button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent)
                    .border(
                        1.dp,
                        if (canSkip) BgCardBorder else BgCardBorder.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(
                        enabled = canSkip,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // show correct answer briefly before skipping
                        correctAnswerText = question?.answer ?: ""
                        answerState = AnswerState.INCORRECT
                        //onSkip()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Skip Question →",
                    color = if (canSkip) TextMuted else TextMuted.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Result feedback banner ────────────────────────────────────────
            AnimatedVisibility(
                visible = answerState != AnswerState.IDLE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (answerState == AnswerState.CORRECT)
                                Green.copy(alpha = 0.15f)
                            else
                                Red.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (answerState == AnswerState.CORRECT) Green.copy(alpha = 0.5f)
                            else Red.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (answerState == AnswerState.CORRECT) "✅ " else "❌ ",
                            fontSize = 18.sp
                        )
                        Column {
                            Text(
                                text = if (answerState == AnswerState.CORRECT)
                                    "Correct!" else "Incorrect",
                                color = if (answerState == AnswerState.CORRECT) Green else Red,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (answerState == AnswerState.CORRECT) {
                                Text("+500 coins", color = Gold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CoinBoosterQueueScreen — waiting for other players
// ─────────────────────────────────────────────────────────────────────────────

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
            animation  = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text     = if (allFinished) "✅" else "⏳",
                fontSize = 52.sp,
                modifier = if (allFinished) Modifier else Modifier.scale(alpha)
            )

            Text(
                text       = if (allFinished) "Everyone finished!" else "Waiting for others...",
                color      = White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Text(
                text      = "${finishedPlayers.size} / $totalPlayers players finished",
                color     = TextMuted,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center
            )

            // progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BgCardBorder)
            ) {
                val progress = if (totalPlayers > 0) finishedPlayers.size.toFloat() / totalPlayers else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Purple, Cyan)))
                )
            }

            // finished players list
            if (finishedPlayers.isNotEmpty()) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text       = "Finished",
                        color      = TextMuted,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    finishedPlayers.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgCard)
                                .border(1.dp, Green.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier         = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Green.copy(alpha = 0.2f))
                                        .border(1.dp, Green.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint               = Green,
                                        modifier           = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text       = player.username,
                                    color      = White,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡ ", fontSize = 12.sp)
                                Text(
                                    text  = "${player.moneyWon} $",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // start board questions button — host only, all finished
            if (isHost && allFinished) {
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
                                onClick           = onStartBoardQuestions
                            )
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = "Start Board Questions →",
                            color      = White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text     = "All players finished — ready to continue!",
                        color    = TextMuted,
                        fontSize = 11.sp
                    )
                }
            } else if (!isHost && allFinished) {
                // non-host sees waiting message
                Text(
                    text      = "Waiting for host to start next phase...",
                    color     = TextMuted,
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FinishedScreen — results + coins earned
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FinishedScreen(
    results: List<GameFinishPlayerResult> = emptyList(),
    myPlayerId: Long = 0L,
    onNavigateHome: () -> Unit = {},
) {
    val myResult = results.find { it.playerId == myPlayerId }
    val coinsEarned = (myResult?.correctAnswers ?: 0) * 500

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("🏆", fontSize = 56.sp)

            Text(
                text = "Game Finished!",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // coins earned card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgGold)
                    .border(1.5.dp, BgGoldBorder, RoundedCornerShape(18.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Coins Earned", color = Gold.copy(alpha = 0.7f), fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙 ", fontSize = 28.sp)
                    Text(
                        text = coinsEarned.toString(),
                        color = Gold,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Leaderboard",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isMe) Purple.copy(alpha = 0.2f) else BgCard
                                    )
                                    .border(
                                        1.dp,
                                        if (isMe) Purple.copy(alpha = 0.6f) else BgCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(medal, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Player ${result.playerId}" + if (isMe) " (You)" else "",
                                        color = if (isMe) Purple else White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪙 ", fontSize = 12.sp)
                                    Text(
                                        text = "${result.correctAnswers * 500}",
                                        color = Gold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Home button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GradButton)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onNavigateHome
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏠  Back to Home",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun CoinBoosterScreenPreview() {
    CoinBoosterScreen(
        username = "Player_123",
        question = CoinBoosterQuestion(
            question = "What is the capital of France?",
            answer = "Paris",
            aliases = listOf("paris")
        ),
        questionIndex = 0,
        totalQuestions = 10,
        correctAnswers = 0,
        coinsBuilt = 0,
        timeLeft = 56,
        onSubmit = { false },
        goToNextQuestion = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun CoinBoosterQueueScreenPreview() {
    CoinBoosterQueueScreen(
        finishedPlayers = listOf(
            CoinBoosterPlayerFinishInfo(playerId = 1L,"matko", moneyWon = 3500f),
            CoinBoosterPlayerFinishInfo(playerId = 2L,"bratko", moneyWon = 4000f),
        ),
        totalPlayers = 4,
        isHost = true,
        {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun FinishedScreenPreview() {
    FinishedScreen(
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