package com.fran.dev.potjera.android.app.game.playervhunter.presentation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.MoneyOffer
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase
import com.fran.dev.potjera.android.app.game.models.state.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.BgGold
import com.fran.dev.potjera.android.app.ui.theme.BgGoldBorder
import com.fran.dev.potjera.android.app.ui.theme.Cyan
import com.fran.dev.potjera.android.app.ui.theme.Gold
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.GradButtonDim
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun PlayerVHunterScreen(
    myPlayerId: Long = 0L,
    isHunter: Boolean = false,
    currentAnsweringPlayerId: Long,
    hunterId: Long,
    boardState: PlayerVHunterBoardState,
    moneyOffer: MoneyOffer? = null,
    allPlayers: List<GameSessionPlayer> = emptyList(),
    onSendMoneyOffer: (higher: Float, lower: Float) -> Unit = { _, _ -> },
    onAcceptOffer: (Float) -> Unit = {},
    onSendAnswer: (String) -> Unit = {},
) {
    val TAG = "PlayerVHunterScreen"

    val isCurrentPlayer = currentAnsweringPlayerId == myPlayerId
    val phase = boardState.boardPhase
    val coinsInPlay = boardState.moneyInGame

    var displayedPlayerPos = boardState.playerStartingIndex + boardState.playerCorrectAnswers

    Log.d(TAG, "PlayerVHunterScreen: displayedPlayerPos: $displayedPlayerPos")
    var displayedHunterPos = boardState.hunterCorrectAnswers - 1

    Log.d(TAG, "PlayerVHunterScreen: displayedHunterPos: $displayedHunterPos")

    // overlay only during active answering — ANSWER_REVEAL is handled by VM
    // which closes overlay and updates board after 1s, then waits for NEW_BOARD_QUESTION
    val showOverlay = phase == BoardPhase.QUESTION_READING ||
            phase == BoardPhase.ANSWER_GIVEN

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopInfoBar(
                coinsInPlay = coinsInPlay,
                stepsToHome = 7 - boardState.playerStartingIndex
            )

            BoardLadder(
                higherOffer = moneyOffer?.higherOffer,
                lowerOffer = moneyOffer?.lowerOffer,
                phase = phase,
                isCurrentPlayer = isCurrentPlayer,
                boardState = boardState,
                displayedPlayerPos = displayedPlayerPos,
                displayedHunterPos = displayedHunterPos,
                onAcceptHigher = { moneyOffer?.higherOffer?.let { onAcceptOffer(it) } },
                onAcceptLower = { moneyOffer?.lowerOffer?.let { onAcceptOffer(it) } },
                onAcceptEarned = { onAcceptOffer(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isHunter && phase == BoardPhase.HUNTER_MAKING_OFFER ->
                    HunterOfferInput(coinsInPlay = coinsInPlay, onSendOffer = onSendMoneyOffer)

                isHunter && phase == BoardPhase.PLAYER_CHOOSING ->
                    StatusCard("⏳", "Player is choosing an offer...", Gold)

                isCurrentPlayer && phase == BoardPhase.PLAYER_CHOOSING ->
                    StatusCard("👆", "Tap an offer above to accept", Purple)

                isCurrentPlayer && phase == BoardPhase.HUNTER_MAKING_OFFER ->
                    StatusCard("🎯", "Hunter is making an offer...", TextMuted)

                !isHunter && !isCurrentPlayer && phase == BoardPhase.HUNTER_MAKING_OFFER ->
                    StatusCard("👀", "Hunter is making an offer...", TextMuted)

                !isHunter && !isCurrentPlayer && phase == BoardPhase.PLAYER_CHOOSING ->
                    StatusCard("👀", "Player is choosing an offer...", TextMuted)

                phase == BoardPhase.OFFER_ACCEPTED ->
                    StatusCard("✅", "Offer accepted! Get ready for the question...", Green)

                else -> {}
            }

            PlayerIndicators(
                allPlayers = allPlayers,
                currentPlayerId = currentAnsweringPlayerId,
                hunterId = hunterId,
                myPlayerId = myPlayerId
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showOverlay) {
            val canAnswer = when {
                isHunter -> boardState.hunterAnswer == null
                isCurrentPlayer -> boardState.playerAnswer == null
                else -> false
            }

            BoardQuestionOverlay(
                boardState = boardState,
                isHunter = isHunter,
                isCurrentPlayer = isCurrentPlayer,
                canAnswer = canAnswer,
                onSendAnswer = onSendAnswer,
            )
        }

        // Board positions update when VM sends new state after ANSWER_REVEAL
        LaunchedEffect(boardState.hunterCorrectAnswers, boardState.playerCorrectAnswers) {
            displayedHunterPos = boardState.hunterCorrectAnswers - 1
            displayedPlayerPos = boardState.playerStartingIndex + boardState.playerCorrectAnswers
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Question overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardQuestionOverlay(
    boardState: PlayerVHunterBoardState,
    isHunter: Boolean,
    isCurrentPlayer: Boolean,
    canAnswer: Boolean,
    onSendAnswer: (String) -> Unit,
) {
    val question = boardState.boardQuestion ?: return
    val phase = boardState.boardPhase
    val labels = listOf("A", "B", "C")

    val correctIndex = question.choices.indexOf(question.correctAnswer)
    val hunterIndex = question.choices.indexOf(boardState.hunterAnswer ?: "")
    val playerIndex = question.choices.indexOf(boardState.playerAnswer ?: "")

    val bothAnswered = boardState.hunterAnswer != null && boardState.playerAnswer != null

    // local reveal animation — runs in the 2s window before backend sends ANSWER_REVEAL
    // which causes the VM to close this overlay
    // step 0 = waiting
    // step 1 = P/H dots appear on chosen answers
    // step 2 = cyan highlight on correct answer
    // step 3 = green on correct, red border on hunter's wrong pick
    var revealStep by remember(question.question) { mutableIntStateOf(0) }

    LaunchedEffect(bothAnswered) {
        if (bothAnswered && revealStep == 0) {
            delay(300)
            revealStep = 1
            delay(500)
            revealStep = 2
            delay(600)
            revealStep = 3
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(1.dp, BgCardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Answered indicators ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = boardState.playerAnswer != null,
                    enter = fadeIn() + expandHorizontally()
                ) { AnsweredBadge("Player", Cyan) }

                if (boardState.playerAnswer == null) Spacer(Modifier.width(1.dp))

                AnimatedVisibility(
                    visible = boardState.hunterAnswer != null,
                    enter = fadeIn() + expandHorizontally()
                ) { AnsweredBadge("Hunter", Color.Red) }
            }

            // ── Question ──────────────────────────────────────────────────────
            Text(
                text = question.question,
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Choices ───────────────────────────────────────────────────────
            question.choices.forEachIndexed { index, choice ->
                val label = labels.getOrElse(index) { "?" }
                val isCorrect = index == correctIndex

                // my own pick — always visible immediately after tapping
                val isMyAnswer = when {
                    isHunter -> index == hunterIndex && boardState.hunterAnswer != null
                    isCurrentPlayer -> index == playerIndex && boardState.playerAnswer != null
                    else -> false
                }

                // opponent picks revealed at step 1
                val isHunterPick =
                    revealStep >= 1 && index == hunterIndex && boardState.hunterAnswer != null
                val isPlayerPick =
                    revealStep >= 1 && index == playerIndex && boardState.playerAnswer != null

                val targetBg = when {
                    revealStep >= 3 && isCorrect -> Green.copy(alpha = 0.25f)
                    revealStep >= 2 && isCorrect -> Cyan.copy(alpha = 0.2f)
                    isMyAnswer -> Purple.copy(alpha = 0.2f)
                    else -> BgDeep
                }

                val targetBorder = when {
                    revealStep >= 3 && isCorrect -> Green
                    revealStep >= 2 && isCorrect -> Cyan
                    revealStep >= 3 && index == hunterIndex
                            && boardState.hunterAnswer != null -> Color.Red

                    isMyAnswer -> Purple
                    else -> BgCardBorder
                }

                val animBg by animateColorAsState(targetBg, tween(400), label = "bg$index")
                val animBorder by animateColorAsState(targetBorder, tween(400), label = "bd$index")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(animBg)
                        .border(1.5.dp, animBorder, RoundedCornerShape(12.dp))
                        .then(
                            if (canAnswer && (phase == BoardPhase.QUESTION_READING || phase == BoardPhase.ANSWER_GIVEN))
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onSendAnswer(choice) }
                            else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isMyAnswer) Purple.copy(0.5f) else Purple.copy(0.25f))
                            .border(
                                1.dp,
                                if (isMyAnswer) Purple else Purple.copy(0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = choice,
                        color = if (canAnswer && (phase == BoardPhase.QUESTION_READING || phase == BoardPhase.ANSWER_GIVEN))
                            White else White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontWeight = if (isMyAnswer) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    // P/H dots appear once revealStep >= 1
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isPlayerPick) AnswerDot("P", Cyan)
                        if (isHunterPick) AnswerDot("H", Color.Red)
                    }
                }
            }

            // ── Status ────────────────────────────────────────────────────────
            val statusText = when {
                revealStep >= 3 -> "✅ Revealed!"
                revealStep >= 1 -> "Revealing..."
                bothAnswered -> "Both answered! Revealing soon..."
                boardState.hunterAnswer != null -> "Hunter answered — waiting for player..."
                boardState.playerAnswer != null -> "Player answered — waiting for hunter..."
                canAnswer -> "👆 Tap an answer"
                isHunter -> "✅ Answered — waiting for player..."
                isCurrentPlayer -> "✅ Answered — waiting for hunter..."
                else -> "👀 Spectating..."
            }

            Text(
                text = statusText,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnsweredBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnswerDot(label: String, color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board ladder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoardLadder(
    higherOffer: Float?,
    lowerOffer: Float?,
    phase: BoardPhase,
    isCurrentPlayer: Boolean,
    boardState: PlayerVHunterBoardState,
    displayedPlayerPos: Int,
    displayedHunterPos: Int,
    onAcceptHigher: () -> Unit,
    onAcceptLower: () -> Unit,
    onAcceptEarned: (amount: Float) -> Unit,
) {
    val steps = listOf(0, 1, 2, 3, 4, 5, 6)
    val isChoosingPhase = phase == BoardPhase.PLAYER_CHOOSING

    // pulsing animation for clickable offer steps
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) { Text("😈", fontSize = 18.sp) }
        Text("HUNTER", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        steps.forEach { step ->
            val isPlayerCurrentStep = step == displayedPlayerPos
            val isHunterCurrentStep = step == displayedHunterPos

            val isBelowHunter = step < displayedHunterPos
            val isBelowPlayer = step in displayedHunterPos..<displayedPlayerPos
            val isAbovePlayer = step > displayedPlayerPos

            // during PLAYER_CHOOSING the first steps from 1-3 become offer slots:
            // step 1 = higher offer  (nearest hunter = riskier start)
            // step 2 = earned amount (middle / default)
            // step 3 = lower offer   (furthest from hunter = safer start)
            val isHigherOfferStep = isChoosingPhase && step == 1 && higherOffer != null
            val isEarnedStep = isChoosingPhase && step == 2
            val isLowerOfferStep = isChoosingPhase && step == 3 && lowerOffer != null

            val isOfferState = isHigherOfferStep || isEarnedStep || isLowerOfferStep
            val isClickableOffer = isOfferState && isCurrentPlayer

            val offerLabel: String
            val offerTextColor: Color
            val offerBorderColor: Color
            when {
                isHigherOfferStep -> {
                    offerLabel = "► ${higherOffer.toInt()}€ ◄"
                    offerTextColor = White
                    offerBorderColor = White
                }

                isEarnedStep -> {
                    offerLabel = "► ${boardState.moneyInGame.toInt()}€ ◄"
                    offerTextColor = White
                    offerBorderColor = White
                }

                isLowerOfferStep -> {
                    offerLabel = "► ${lowerOffer.toInt()}€ ◄"
                    offerTextColor = White
                    offerBorderColor = White
                }

                else -> {
                    offerLabel = ""
                    offerTextColor = Color.Transparent
                    offerBorderColor = Color.Transparent
                }
            }

            val targetBg = when {
                isChoosingPhase && isOfferState -> Color(0xFF06C8C8)
                isBelowHunter -> Color(0xFFCC1A1A)
                isHunterCurrentStep && !isPlayerCurrentStep -> Color(0xFFCC1A1A)
                isBelowPlayer -> Color(0xFF06C8C8)
                isAbovePlayer -> Color(0xFF1A1A8A)
                else -> Color(0xFF06C8C8)
            }

            val animBg by animateColorAsState(
                targetValue = targetBg,
                animationSpec = tween(500, easing = EaseInOut),
                label = "ladder_$step"
            )

            val scaleModifier = if (isClickableOffer) Modifier.scale(pulseScale) else Modifier

            Box(
                modifier = scaleModifier
                    .fillMaxWidth(0.75f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(animBg)
                    .then(
                        if (isOfferState) Modifier.border(
                            width = if (isClickableOffer) 2.dp else 1.dp,
                            color = offerBorderColor.copy(
                                alpha = if (isClickableOffer) pulseBorderAlpha else 0.3f
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
                    .then(
                        if (isClickableOffer) Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            when {
                                isHigherOfferStep -> onAcceptHigher()
                                isLowerOfferStep -> onAcceptLower()
                                // earned = accept current moneyInGame = lower path (no change)
                                isEarnedStep -> onAcceptEarned(boardState.moneyInGame)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isOfferState -> {
                        Text(
                            text = offerLabel,
                            color = offerTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    isHunterCurrentStep -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                            Text(
                                "▼",
                                color = Color(0xFF1A0000),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    isPlayerCurrentStep -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "► ",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${boardState.moneyInGame.toInt()}€",
                                color = White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                " ◄",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {}
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Text("🏠  HOME", color = White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Purple),
            contentAlignment = Alignment.Center
        ) { Text("🎮", fontSize = 20.sp) }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Hunter offer input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HunterOfferInput(
    coinsInPlay: Float,
    onSendOffer: (higher: Float, lower: Float) -> Unit,
) {
    var higherInput by remember { mutableStateOf("") }
    var lowerInput by remember { mutableStateOf("") }
    val isValid = higherInput.isNotBlank() && lowerInput.isNotBlank() &&
            (higherInput.toFloatOrNull() ?: 0f) > coinsInPlay &&
            (lowerInput.toFloatOrNull() ?: 0f) < coinsInPlay

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Make your offer", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Current coins in play: ${coinsInPlay.toInt()}", color = TextMuted, fontSize = 12.sp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🔼 Higher offer (must be > ${coinsInPlay.toInt()})",
                color = Green,
                fontSize = 12.sp
            )
            OfferTextField(
                value = higherInput,
                onValueChange = {
                    if (it.isEmpty() || it == "-" || it.toFloatOrNull() != null) {
                        higherInput = it
                    }
                },
                placeholder = "e.g. ${(coinsInPlay * 1.5f).toInt()}",
                isValid = higherInput.isNotBlank() && (higherInput.toFloatOrNull()
                    ?: 0f) > coinsInPlay,
                validColor = Green
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🔽 Lower offer (must be < ${coinsInPlay.toInt()})",
                color = Color.Red,
                fontSize = 12.sp
            )
            OfferTextField(
                value = lowerInput,
                onValueChange = {
                    if (it.isEmpty() || it == "-" || it.toFloatOrNull() != null) {
                        lowerInput = it
                    }
                },
                placeholder = "e.g. ${(coinsInPlay * 0.5f).toInt()}",
                isValid = lowerInput.isNotBlank() && (lowerInput.toFloatOrNull()
                    ?: 0f) < coinsInPlay,
                validColor = Color.Red
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isValid) GradButton else GradButtonDim)
                .clickable(
                    enabled = isValid,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSendOffer(higherInput.toFloat(), lowerInput.toFloat()) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Send Offer", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OfferTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isValid: Boolean,
    validColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgDeep)
            .border(
                1.dp,
                if (isValid) validColor.copy(alpha = 0.6f) else BgCardBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = TextMuted.copy(alpha = 0.5f), fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val filtered = newValue.filter { c -> c.isDigit() || c == '-' || c == '.' }
                // Only allow '-' as the first character
                val sanitized = if (filtered.startsWith('-')) {
                    "-" + filtered.drop(1).filter { c -> c.isDigit() || c == '.' }
                } else {
                    filtered
                }
                onValueChange(sanitized)
            },
            textStyle = TextStyle(color = White, fontSize = 14.sp),
            cursorBrush = SolidColor(Purple),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(emoji: String, message: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(message, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player indicators
// ─────────────────────────────────────────────────────────────────────────────


@Composable
private fun PlayerIndicators(
    allPlayers: List<GameSessionPlayer>,
    currentPlayerId: Long?,
    hunterId: Long?,
    myPlayerId: Long,
) {
    val nonHunterPlayers = allPlayers.filter { it.playerId != hunterId }
    val totalEarned = nonHunterPlayers.sumOf { it.moneyWon.toDouble() }.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Players", color = TextMuted, fontSize = 11.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nonHunterPlayers.forEach { player ->
                val isActive = player.playerId == currentPlayerId
                val isMe = player.playerId == myPlayerId

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 48.dp else 38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> Purple
                                    isMe -> Purple.copy(alpha = 0.5f)
                                    else -> BgCard
                                }
                            )
                            .border(2.dp, if (isActive) Purple else BgCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMe) "🎮" else "👤",
                            fontSize = if (isActive) 20.sp else 16.sp
                        )
                    }
                    Text(
                        text = if (isMe) "${player.playerName} (you)" else player.playerName,
                        color = if (isActive) Purple else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) Text("▲", color = Purple, fontSize = 10.sp)
                }
            }
        }

        // ── Total earned bar ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .border(1.dp, BgGoldBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BgGold)
                        .border(1.dp, BgGoldBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪙", fontSize = 13.sp)
                }
                Text(
                    text = "Team earnings",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${totalEarned.toInt()}€",
                color = Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top info bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopInfoBar(coinsInPlay: Float, stepsToHome: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Playing for", color = TextMuted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 ", fontSize = 14.sp)
                Text(
                    text = coinsInPlay.toInt().toString(),
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Steps to Home", color = TextMuted, fontSize = 11.sp)
            Text(
                text = stepsToHome.toString(),
                color = White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
