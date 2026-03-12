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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.presentation.HunterAnsweringPhaseState
import com.fran.dev.potjera.android.app.game.presentation.PlayersAnsweringPlayer
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgInput
import com.fran.dev.potjera.android.app.ui.theme.Cyan
import com.fran.dev.potjera.android.app.ui.theme.GradButtonDim
import com.fran.dev.potjera.android.app.ui.theme.Green
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.Red
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White
import kotlinx.coroutines.delay
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

data class SuggestionItem(
    val playerId: Long,
    val username: String,
    val suggestion: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// HunterPhaseScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HunterPhaseScreen(
    phaseName: String = "Hunter Phase",
    phaseSubtitle: String = "Catch them all",
    state: HunterAnsweringPhaseState,
    isHunter: Boolean = false,
    isCaptain: Boolean = false,
    isSpectator: Boolean = false,
    suggestions: List<SuggestionItem> = emptyList(),
    captainId: Long? = null,
    onAnswer: (answer: String) -> Unit = {},
    onSuggest: (suggestion: String) -> Unit = {},
) {
    var timerSeconds by remember { mutableIntStateOf(120) }
    var answer by remember { mutableStateOf("") }
    var suggestionInput by remember { mutableStateOf("") }

    LaunchedEffect(state.question) { answer = "" }
    LaunchedEffect(state.question) { suggestionInput = "" }

    LaunchedEffect(state.endTimestamp, state.playersAreAnswering) {

        while (true) {

            if (!state.playersAreAnswering) {

                val remainingMs = state.endTimestamp - System.currentTimeMillis()
                val remainingSeconds = (remainingMs / 1000).toInt()

                timerSeconds = max(remainingSeconds, 0)
            }

            delay(250)
        }
    }

    val mm = timerSeconds / 60
    val ss = timerSeconds % 60
    val timerText = "%d:%02d".format(mm, ss)
    val timerLow = timerSeconds < 30

    val isRegularPlayer = !isHunter && !isCaptain && !isSpectator

    // feedback card visible whenever any answer result exists
    // true = someone got it right, false = both got it wrong
    val feedbackCorrect: Boolean? = when {
        state.hunterAnsweredCorrectly == true -> true
        state.playersAnsweredCorrectly == true -> true
        state.hunterAnsweredCorrectly == false
                && state.playersAnsweredCorrectly == false -> false

        state.hunterAnsweredCorrectly == false
                && state.playersAnsweredCorrectly == null -> null // players still answering, no reveal yet
        else -> null
    }

    // correct answer revealed when both sides have answered OR hunter got it right
    val showCorrectAnswer = state.correctAnswer != null && (
            state.hunterAnsweredCorrectly == true ||
                    state.playersAnsweredCorrectly != null
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        //region ── Header ──────────────────────────────────────────────────────
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
                ) { Text("🎯", fontSize = 18.sp) }
                Column {
                    Text(phaseName, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(phaseSubtitle, color = TextMuted, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.5.dp, if (timerLow) Red else BgCardBorder, RoundedCornerShape(12.dp))
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
        //endregion

        //region ── Players card ────────────────────────────────────────────────
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
                state.players.forEach { member ->
                    TeamMemberItem(
                        member = member,
                        isCaptain = member.playerId == captainId,
                    )
                }
            }
        }
        //endregion

        //region ── Hunter progress ─────────────────────────────────────────────
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
                        "${state.hunterCorrectAnswers} / ${state.playersSteps}",
                        color = Color(0xFFFF6B00),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
            ) {
                for (i in 1..state.playersSteps) {
                    val isCaught = i <= state.hunterCorrectAnswers
                    val bgColor = if (isCaught) Color(0xFFFF6B00) else Color(0xFF3A7FFF)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (i == 1) 8.dp else 0.dp,
                                    bottomStart = if (i == 1) 8.dp else 0.dp,
                                    topEnd = if (i == state.playersSteps) 8.dp else 0.dp,
                                    bottomEnd = if (i == state.playersSteps) 8.dp else 0.dp,
                                )
                            )
                            .background(bgColor)
                            .border(2.dp, White.copy(alpha = .4f), RoundedCornerShape(8.dp)),
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

            val progress = if (state.playersSteps > 0)
                state.hunterCorrectAnswers.toFloat() / state.playersSteps else 0f
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
                            Brush.horizontalGradient(listOf(Color(0xFFFF6B00), Color(0xFFFFB703)))
                        )
                )
            }
        }
        //endregion

        //region ── Answer feedback ─────────────────────────────────────────────
        // Shows when hunter got it right OR when both sides have answered (right or wrong)
        AnimatedVisibility(
            visible = showCorrectAnswer,
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
                        if (feedbackCorrect == true) Green.copy(alpha = 0.4f)
                        else Red.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (feedbackCorrect == true) "Correct answer!" else "Correct answer:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = state.correctAnswer ?: "",
                        color = if (feedbackCorrect == true) Green else Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        //endregion

        //region ── Question card ───────────────────────────────────────────────
        if (state.question.isNotBlank()) {
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
                    state.question,
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }
        //endregion

        //region ── Role-based action area ──────────────────────────────────────
        when {
            isHunter -> {
                when (state.hunterAnsweredCorrectly) {
                    true -> { /* feedback card handles it */
                    }

                    false -> HunterWrongBanner()
                    null -> if (!showCorrectAnswer) {
                        HunterAnswerInput(
                            answer = answer,
                            onChange = { answer = it },
                            onSubmit = { onAnswer(answer) }
                        )
                    }
                }
            }

            isCaptain -> {
                CaptainSection(
                    captainInputOpen = state.hunterAnsweredCorrectly == false
                            && state.playersAreAnswering
                            && !showCorrectAnswer,
                    answer = answer,
                    suggestions = suggestions,
                    onChange = { answer = it },
                    onSubmit = { onAnswer(answer) }
                )
            }

            isRegularPlayer -> {
                when {
                    // players phase open — show suggestion input
                    state.playersAreAnswering && !showCorrectAnswer -> {
                        PlayerSuggestionInput(
                            input = suggestionInput,
                            suggestions = suggestions,
                            inputEnabled = true,
                            onChange = { suggestionInput = it },
                            onSend = {
                                if (suggestionInput.isNotBlank()) {
                                    onSuggest(suggestionInput)
                                    suggestionInput = ""
                                }
                            }
                        )
                    }
                    // hunter still answering — show status card
                    state.hunterIsAnswering -> WaitingForHunterBanner()
                    // answer revealed — nothing extra needed (feedback card above handles it)
                    else -> {}
                }
            }

            isSpectator -> SpectatorBanner()
        }
        //endregion

        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

//region ── Hunter: normal answer input ────────────────────────────────────────
@Composable
private fun HunterAnswerInput(
    answer: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
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
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgInput)
                .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFFFF6B00)),
            decorationBox = { inner ->
                if (answer.isEmpty()) Text("Type your answer…", color = TextMuted, fontSize = 16.sp)
                inner()
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B00), Color(0xFFFFB703))))
                .clickable(onClick = onSubmit),
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
}
//endregion

//region ── Hunter: answered wrong — players are now answering ─────────────────
@Composable
private fun HunterWrongBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("❌", fontSize = 30.sp)
            Text(
                "You answered wrong!",
                color = Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Players are now giving the answer…",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
//endregion

//region ── Regular player / spectator: hunter still answering ─────────────────
@Composable
private fun WaitingForHunterBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", fontSize = 22.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Hunter is answering",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Wait for the hunter to give their answer…",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
//endregion

//region ── Captain section ────────────────────────────────────────────────────
@Composable
private fun CaptainSection(
    captainInputOpen: Boolean,
    answer: String,
    suggestions: List<SuggestionItem>,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        if (captainInputOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, Cyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👑", fontSize = 16.sp)
                    Text(
                        "CAPTAIN'S ANSWER",
                        color = Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Text("Hunter got it wrong — it's your turn!", color = TextMuted, fontSize = 12.sp)
                BasicTextField(
                    value = answer,
                    onValueChange = onChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgInput)
                        .border(1.dp, Cyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Cyan),
                    decorationBox = { inner ->
                        if (answer.isEmpty()) Text(
                            "Type team answer…",
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
                                listOf(
                                    Cyan.copy(alpha = 0.8f),
                                    Purple
                                )
                            )
                        )
                        .clickable(onClick = onSubmit),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SUBMIT TEAM ANSWER",
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👑", fontSize = 18.sp)
                    Column {
                        Text(
                            "You are the Captain",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Waiting for hunter to answer first…",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            SuggestionsPanel(suggestions = suggestions)
        }
    }
}
//endregion

//region ── Regular player: suggestion input + sees all suggestions below ──────
@Composable
private fun PlayerSuggestionInput(
    input: String,
    suggestions: List<SuggestionItem>,
    inputEnabled: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, Purple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💡", fontSize = 14.sp)
                Text(
                    "SUGGEST TO CAPTAIN",
                    color = Purple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                "You can't answer directly — suggest an answer to the captain",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = if (inputEnabled) onChange else { _ -> },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (inputEnabled) BgInput else BgInput.copy(alpha = 0.5f))
                        .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        color = if (inputEnabled) White else TextMuted,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Purple),
                    singleLine = true,
                    enabled = inputEnabled,
                    decorationBox = { inner ->
                        if (input.isEmpty()) Text(
                            if (inputEnabled) "Your suggestion…" else "Waiting…",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                        inner()
                    }
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (inputEnabled) Purple else Purple.copy(alpha = 0.3f))
                        .clickable(enabled = inputEnabled, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Text("➤", color = White, fontSize = 18.sp)
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            SuggestionsPanel(suggestions = suggestions)
        }
    }
}
//endregion

//region ── Shared suggestions panel ──────────────────────────────────────────
@Composable
private fun SuggestionsPanel(suggestions: List<SuggestionItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, Purple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("💬", fontSize = 14.sp)
            Text(
                "TEAM SUGGESTIONS",
                color = Purple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        suggestions.forEach { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgDeep)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Purple.copy(alpha = 0.2f))
                        .border(1.dp, Purple.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("👤", fontSize = 12.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.username, color = TextMuted, fontSize = 11.sp)
                    Text(
                        s.suggestion,
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
//endregion

//region ── Spectator ──────────────────────────────────────────────────────────
@Composable
private fun SpectatorBanner() {
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
//endregion

//region ── Player avatar in the players row ───────────────────────────────────
@Composable
private fun TeamMemberItem(
    member: PlayersAnsweringPlayer,
    isCaptain: Boolean,
) {
    val borderColor = if (isCaptain) Cyan else BgCardBorder
    val bgColor = if (isCaptain) Cyan.copy(alpha = 0.1f) else BgDeep

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.emoji, fontSize = 22.sp)
        }
        Text(
            member.name.take(10),
            color = White,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (isCaptain) {
            Text("👑 Captain", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
//endregion

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private val previewPlayers = listOf(
    PlayersAnsweringPlayer(1, "Player_123", "🎮"),
    PlayersAnsweringPlayer(2, "QuizMaster", "🧠"),
    PlayersAnsweringPlayer(3, "FastAnswerer", "⚡"),
    PlayersAnsweringPlayer(4, "Shadow", "🔥"),
)

private val previewSuggestions = listOf(
    SuggestionItem(1, "Player_123", "Paris"),
    SuggestionItem(3, "FastAnswerer", "Rome"),
)

// ── 1. Hunter — answering ─────────────────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "1. Hunter - answering"
)
@Composable
fun PreviewHunter() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterIsAnswering = true
        ),
        isHunter = true, captainId = 2L,
    )
}

// ── 2. Hunter — answered wrong ────────────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "2. Hunter - answered wrong"
)
@Composable
fun PreviewHunterWrong() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterAnsweredCorrectly = false,
            hunterIsAnswering = false,
            playersAreAnswering = true
        ),
        isHunter = true, captainId = 2L,
    )
}

// ── 3. Hunter — answered correctly ───────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "3. Hunter - answered correctly"
)
@Composable
fun PreviewHunterCorrect() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 4,
            question = "What is the capital of France?",
            correctAnswer = "Paris",
            hunterAnsweredCorrectly = true
        ),
        isHunter = true, captainId = 2L,
    )
}

// ── 4. Captain — waiting for hunter ──────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "4. Captain - waiting for hunter"
)
@Composable
fun PreviewCaptainWaiting() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterIsAnswering = true
        ),
        isCaptain = true, suggestions = emptyList(), captainId = 2L,
    )
}

// ── 5. Captain — input open, no suggestions ───────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "5. Captain - input open, no suggestions"
)
@Composable
fun PreviewCaptainInputOpen() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterAnsweredCorrectly = false,
            hunterIsAnswering = false,
            playersAreAnswering = true
        ),
        isCaptain = true, suggestions = emptyList(), captainId = 2L,
    )
}

// ── 6. Captain — input open with suggestions ─────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 900,
    name = "6. Captain - input open with suggestions"
)
@Composable
fun PreviewCaptainWithSuggestions() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterAnsweredCorrectly = false,
            hunterIsAnswering = false,
            playersAreAnswering = true
        ),
        isCaptain = true, suggestions = previewSuggestions, captainId = 2L,
    )
}

// ── 7. Regular player — hunter answering (waiting banner) ─────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "7. Regular player - hunter answering"
)
@Composable
fun PreviewRegularPlayerHunterAnswering() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterIsAnswering = true,
            playersAreAnswering = false
        ),
        captainId = 2L,
    )
}

// ── 8. Regular player — players phase, suggestion input ───────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "8. Regular player - suggestion input"
)
@Composable
fun PreviewRegularPlayerSuggesting() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterAnsweredCorrectly = false,
            hunterIsAnswering = false,
            playersAreAnswering = true
        ),
        captainId = 2L, suggestions = previewSuggestions,
    )
}

// ── 9. Both wrong — correct answer revealed ───────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "9. Both wrong - correct answer revealed"
)
@Composable
fun PreviewBothWrong() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            correctAnswer = "Paris",
            hunterAnsweredCorrectly = false,
            playersAnsweredCorrectly = false
        ),
        isCaptain = true, suggestions = previewSuggestions, captainId = 2L,
    )
}

// ── 10. Players answered correctly ────────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "10. Players answered correctly"
)
@Composable
fun PreviewPlayersCorrect() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            correctAnswer = "Paris",
            hunterAnsweredCorrectly = false,
            playersAnsweredCorrectly = true
        ),
        captainId = 2L, suggestions = previewSuggestions,
    )
}

// ── 11. Spectator ─────────────────────────────────────────────────────────────
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1035,
    widthDp = 390,
    heightDp = 820,
    name = "11. Spectator"
)
@Composable
fun PreviewSpectator() {
    HunterPhaseScreen(
        state = HunterAnsweringPhaseState(
            players = previewPlayers,
            playersSteps = 7,
            hunterCorrectAnswers = 3,
            question = "What is the capital of France?",
            hunterIsAnswering = true
        ),
        isSpectator = true, captainId = 2L,
    )
}
