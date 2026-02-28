package com.fran.dev.potjera.android.app.game.playervhunter.presentation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.services.BoardPhaseStartingDto
import com.fran.dev.potjera.android.app.game.services.CoinBoosterPlayerStateDto
import com.fran.dev.potjera.android.app.game.services.CurrentPlayerInfoDto
import com.fran.dev.potjera.android.app.game.services.MoneyOfferAcceptedDto
import com.fran.dev.potjera.android.app.game.services.MoneyOfferDto
import com.fran.dev.potjera.android.app.ui.theme.*

enum class BoardOfferPhase {
    HUNTER_MAKING_OFFER,   // hunter is typing offers
    PLAYER_CHOOSING,       // player is choosing higher/lower
    OFFER_ACCEPTED         // offer accepted, waiting for question
}

@Composable
fun PlayerVHunterScreen(
    myPlayerId: Long = 0L,
    isHunter: Boolean = false,
    boardState: BoardPhaseStartingDto? = null,
    currentPlayerInfo: CurrentPlayerInfoDto? = null,
    moneyOffer: MoneyOfferDto? = null,
    moneyOfferAccepted: MoneyOfferAcceptedDto? = null,
    allPlayers: List<CoinBoosterPlayerStateDto> = emptyList(),
    onSendMoneyOffer: (higher: Float, lower: Float) -> Unit = { _, _ -> },
    onAcceptOffer: (Float) -> Unit = {},
) {
    val isCurrentPlayer = boardState?.currentPlayerId == myPlayerId

    val offerPhase = when {
        moneyOfferAccepted != null -> BoardOfferPhase.OFFER_ACCEPTED
        moneyOffer != null         -> BoardOfferPhase.PLAYER_CHOOSING
        else                       -> BoardOfferPhase.HUNTER_MAKING_OFFER
    }

    val coinsInPlay = currentPlayerInfo?.coinsEarned?.toFloat() ?: 5000f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            TopInfoBar(
                coinsInPlay   = coinsInPlay,
                stepsToHome   = currentPlayerInfo?.correctAnswers ?: 0
            )

            // ── Board ladder ─────────────────────────────────────────────────
            BoardLadder(
                coinsInPlay        = coinsInPlay,
                higherOffer        = moneyOffer?.higherOffer,
                lowerOffer         = moneyOffer?.lowerOffer,
                acceptedOffer      = moneyOfferAccepted?.acceptedOffer,
                offerPhase         = offerPhase,
                isCurrentPlayer    = isCurrentPlayer,
                onAcceptHigher     = { moneyOffer?.higherOffer?.let { onAcceptOffer(it) } },
                onAcceptLower      = { moneyOffer?.lowerOffer?.let { onAcceptOffer(it) } }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Status / action area ──────────────────────────────────────────
            when {
                // hunter enters offer
                isHunter && offerPhase == BoardOfferPhase.HUNTER_MAKING_OFFER -> {
                    HunterOfferInput(
                        coinsInPlay    = coinsInPlay,
                        onSendOffer    = onSendMoneyOffer
                    )
                }

                // hunter waiting for player to choose
                isHunter && offerPhase == BoardOfferPhase.PLAYER_CHOOSING -> {
                    StatusCard(
                        emoji   = "⏳",
                        message = "Player is choosing an offer...",
                        color   = Gold
                    )
                }

                // current player sees offer choices
                isCurrentPlayer && offerPhase == BoardOfferPhase.PLAYER_CHOOSING -> {
                    // handled in BoardLadder — clickable rows
                    StatusCard(
                        emoji   = "👆",
                        message = "Tap an offer above to accept",
                        color   = Purple
                    )
                }

                // current player waiting for hunter
                isCurrentPlayer && offerPhase == BoardOfferPhase.HUNTER_MAKING_OFFER -> {
                    StatusCard(
                        emoji   = "🎯",
                        message = "Hunter is making an offer...",
                        color   = TextMuted
                    )
                }

                // spectator
                !isHunter && !isCurrentPlayer && offerPhase == BoardOfferPhase.HUNTER_MAKING_OFFER -> {
                    StatusCard(
                        emoji   = "👀",
                        message = "Hunter is making an offer...",
                        color   = TextMuted
                    )
                }

                !isHunter && !isCurrentPlayer && offerPhase == BoardOfferPhase.PLAYER_CHOOSING -> {
                    StatusCard(
                        emoji   = "👀",
                        message = "Player is choosing an offer...",
                        color   = TextMuted
                    )
                }

                offerPhase == BoardOfferPhase.OFFER_ACCEPTED -> {
                    StatusCard(
                        emoji   = "✅",
                        message = "Offer accepted! Get ready for the question...",
                        color   = Green
                    )
                }

                else -> {}
            }

            // ── Player indicators ─────────────────────────────────────────────
            PlayerIndicators(
                allPlayers      = allPlayers,
                currentPlayerId = boardState?.currentPlayerId,
                hunterId        = boardState?.hunterId,
                myPlayerId      = myPlayerId
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top info bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopInfoBar(coinsInPlay: Float, stepsToHome: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("Playing for", color = TextMuted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 ", fontSize = 14.sp)
                Text(
                    text       = coinsInPlay.toInt().toString(),
                    color      = Gold,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Steps to Home", color = TextMuted, fontSize = 11.sp)
            Text(
                text       = stepsToHome.toString(),
                color      = White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Board ladder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoardLadder(
    coinsInPlay: Float,
    higherOffer: Float?,
    lowerOffer: Float?,
    acceptedOffer: Float?,
    offerPhase: BoardOfferPhase,
    isCurrentPlayer: Boolean,
    onAcceptHigher: () -> Unit,
    onAcceptLower: () -> Unit,
) {
    val steps = listOf(5, 4, 3, 2, 1)

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // hunter avatar at top
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Text("😈", fontSize = 20.sp)
        }
        Text("HUNTER", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(4.dp))

        // higher offer row — clickable for current player
        if (higherOffer != null && offerPhase == BoardOfferPhase.PLAYER_CHOOSING) {
            OfferRow(
                label        = "🔼  Higher: ${higherOffer.toInt()} coins",
                color        = if (acceptedOffer == higherOffer) Green else Color(0xFF2ECC71).copy(alpha = 0.3f),
                borderColor  = Green,
                isClickable  = isCurrentPlayer && acceptedOffer == null,
                onClick      = onAcceptHigher
            )
        }

        // steps rows
        steps.forEach { step ->
            val isCurrentStep = step == 4  // example: highlight current position
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isCurrentStep -> Color(0xFF06C8C8)
                            else          -> Color(0xFF3A3ADB)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentStep) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙 ", fontSize = 14.sp)
                        Text(
                            text       = "${coinsInPlay.toInt()}",
                            color      = White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Text(
                        text      = step.toString(),
                        color     = White.copy(alpha = 0.6f),
                        fontSize  = 14.sp
                    )
                }
            }
        }

        // HOME row
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "🏠  HOME",
                color      = White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // lower offer row — clickable for current player
        if (lowerOffer != null && offerPhase == BoardOfferPhase.PLAYER_CHOOSING) {
            Spacer(modifier = Modifier.height(4.dp))
            OfferRow(
                label        = "🔽  Lower: ${lowerOffer.toInt()} coins",
                color        = if (acceptedOffer == lowerOffer) Color.Red else Color.Red.copy(alpha = 0.2f),
                borderColor  = Color.Red.copy(alpha = 0.6f),
                isClickable  = isCurrentPlayer && acceptedOffer == null,
                onClick      = onAcceptLower
            )
        }

        // player avatar at bottom
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Purple),
            contentAlignment = Alignment.Center
        ) {
            Text("🎮", fontSize = 20.sp)
        }
    }
}

@Composable
private fun OfferRow(
    label: String,
    color: Color,
    borderColor: Color,
    isClickable: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .then(
                if (isClickable) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClick
                ) else Modifier
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold
        )
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
    var lowerInput  by remember { mutableStateOf("") }
    val isValid     = higherInput.isNotBlank() && lowerInput.isNotBlank() &&
            (higherInput.toFloatOrNull() ?: 0f) > coinsInPlay &&
            (lowerInput.toFloatOrNull() ?: 0f) < coinsInPlay

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = "Make your offer",
            color      = White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text     = "Current coins in play: ${coinsInPlay.toInt()}",
            color    = TextMuted,
            fontSize = 12.sp
        )

        // higher offer input
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔼 Higher offer (must be > ${coinsInPlay.toInt()})", color = Green, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgDeep)
                    .border(
                        1.dp,
                        if (higherInput.isNotBlank() && (higherInput.toFloatOrNull() ?: 0f) > coinsInPlay)
                            Green.copy(alpha = 0.6f) else BgCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (higherInput.isEmpty()) {
                    Text("e.g. ${(coinsInPlay * 1.5f).toInt()}", color = TextMuted.copy(alpha = 0.5f), fontSize = 14.sp)
                }
                BasicTextField(
                    value         = higherInput,
                    onValueChange = { higherInput = it.filter { c -> c.isDigit() } },
                    textStyle     = TextStyle(color = White, fontSize = 14.sp),
                    cursorBrush   = SolidColor(Purple),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }

        // lower offer input
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔽 Lower offer (must be < ${coinsInPlay.toInt()})", color = Color.Red, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgDeep)
                    .border(
                        1.dp,
                        if (lowerInput.isNotBlank() && (lowerInput.toFloatOrNull() ?: 0f) < coinsInPlay)
                            Color.Red.copy(alpha = 0.6f) else BgCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (lowerInput.isEmpty()) {
                    Text("e.g. ${(coinsInPlay * 0.5f).toInt()}", color = TextMuted.copy(alpha = 0.5f), fontSize = 14.sp)
                }
                BasicTextField(
                    value         = lowerInput,
                    onValueChange = { lowerInput = it.filter { c -> c.isDigit() } },
                    textStyle     = TextStyle(color = White, fontSize = 14.sp),
                    cursorBrush   = SolidColor(Purple),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }

        // send button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isValid) GradButton else GradButtonDim)
                .clickable(
                    enabled           = isValid,
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onSendOffer(
                        higherInput.toFloat(),
                        lowerInput.toFloat()
                    )
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Send Offer",
                color      = White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            text       = message,
            color      = color,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player indicators at bottom
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerIndicators(
    allPlayers: List<CoinBoosterPlayerStateDto>,
    currentPlayerId: Long?,
    hunterId: Long?,
    myPlayerId: Long,
) {
    val nonHunterPlayers = allPlayers.filter { it.playerId != hunterId }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = "Players",
            color    = TextMuted,
            fontSize = 11.sp
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nonHunterPlayers.forEach { player ->
                val isActive = player.playerId == currentPlayerId
                val isMe     = player.playerId == myPlayerId

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
                                    isMe     -> Purple.copy(alpha = 0.5f)
                                    else     -> BgCard
                                }
                            )
                            .border(
                                2.dp,
                                if (isActive) Purple else BgCardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = if (isMe) "🎮" else "👤",
                            fontSize = if (isActive) 20.sp else 16.sp
                        )
                    }
                    if (isActive) {
                        Text(
                            text     = "▲",
                            color    = Purple,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
