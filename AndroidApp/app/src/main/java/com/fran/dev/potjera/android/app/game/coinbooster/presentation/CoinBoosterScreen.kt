package com.fran.dev.potjera.android.app.game.coinbooster.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgGold
import com.fran.dev.potjera.android.app.ui.theme.BgGoldBorder
import com.fran.dev.potjera.android.app.ui.theme.BgInput
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.GradButtonDim
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.Red
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White
import kotlinx.coroutines.delay

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
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                    Box(
                        modifier = Modifier.Companion
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Purple),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Text("🎮", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.Companion.width(10.dp))
                    Text(
                        text = username,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Companion.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.Companion.End) {
                    Text("Time Left", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "${timeLeft}s",
                        color = if (timeLeft <= 10) Red else White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Companion.ExtraBold
                    )
                }
            }

            // ── Stats row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.Companion
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(
                            1.dp,
                            BgCardBorder,
                            androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                ) {
                    Text("⚡", fontSize = 22.sp)
                    Spacer(modifier = Modifier.Companion.height(6.dp))
                    Text(
                        text = "$correctAnswers/$totalQuestions",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Companion.ExtraBold
                    )
                    Text("Correct", color = TextMuted, fontSize = 12.sp)
                }

                Column(
                    modifier = Modifier.Companion
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(BgGold)
                        .border(
                            1.dp,
                            BgGoldBorder,
                            androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                ) {
                    Text("🪙", fontSize = 22.sp)
                    Spacer(modifier = Modifier.Companion.height(6.dp))
                    Text(
                        text = coinsBuilt.toString(),
                        color = Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Companion.ExtraBold
                    )
                    Text("Coins Built", color = Gold.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            // ── Progress bar ─────────────────────────────────────────────────
            val progress = if (totalQuestions > 0) questionIndex.toFloat() / totalQuestions else 0f
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(BgCardBorder)
            ) {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(Purple)
                )
            }

            // ── Question label ───────────────────────────────────────────────
            Text(
                text = "Question ${questionIndex + 1} of $totalQuestions",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.fillMaxWidth()
            )

            // ── Question card ────────────────────────────────────────────────
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(
                        1.dp,
                        BgCardBorder,
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = question?.question ?: "Loading...",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    textAlign = TextAlign.Companion.Center,
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
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(BgInput)
                    .border(
                        1.5.dp,
                        inputBorderColor,
                        androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    )
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
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (answerState == AnswerState.IDLE) inputText = it },
                        textStyle = TextStyle(color = White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Purple),
                        singleLine = true,
                        modifier = Modifier.Companion.weight(1f)
                    )
                    if (answerState != AnswerState.IDLE) {
                        Icon(
                            imageVector = if (answerState == AnswerState.CORRECT)
                                Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (answerState == AnswerState.CORRECT) Green else Red,
                            modifier = Modifier.Companion.size(20.dp)
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
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(
                            1.dp,
                            if (answerState == AnswerState.CORRECT) Green.copy(alpha = 0.4f)
                            else Red.copy(alpha = 0.4f),
                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                            fontWeight = FontWeight.Companion.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.weight(1f))

            // ── Submit button ─────────────────────────────────────────────────
            val canSubmit = answerState == AnswerState.IDLE && inputText.isNotBlank()
            val canSkip = answerState == AnswerState.IDLE

            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
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
                contentAlignment = Alignment.Companion.Center
            ) {
                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                    Text("✈️  ", fontSize = 15.sp)
                    Text(
                        text = "Submit Answer",
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Companion.Bold
                    )
                }
            }

            // ── Skip button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(Color.Companion.Transparent)
                    .border(
                        1.dp,
                        if (canSkip) BgCardBorder else BgCardBorder.copy(alpha = 0.3f),
                        androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
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
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = "Skip Question →",
                    color = if (canSkip) TextMuted else TextMuted.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Companion.Medium
                )
            }

            // ── Result feedback banner ────────────────────────────────────────
            AnimatedVisibility(
                visible = answerState != AnswerState.IDLE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
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
                            androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Row(verticalAlignment = Alignment.Companion.CenterVertically) {
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
                                fontWeight = FontWeight.Companion.Bold
                            )
                            if (answerState == AnswerState.CORRECT) {
                                Text("+500 coins", color = Gold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))
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
