package com.fran.dev.potjera.android.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BgDeep        = Color(0xFF1A1035)
val BgCard        = Color(0xFF251848)
val BgCardBorder  = Color(0xFF3A2A6A)
val BgInput       = Color(0xFF1A1035)
val BgHunterCard  = Color(0xFF2A1020)
val BgHunterBorder = Color(0xFF6A1030)
val BgGold        = Color(0xFF3D2800)
val BgGoldBorder  = Color(0xFF8B5E00)
val Gold          = Color(0xFFF5A623)
val Purple        = Color(0xFF9B59FC)
val Green         = Color(0xFF2ECC71)
val Red           = Color(0xFFE74C3C)
val Cyan          = Color(0xFF06C8C8)
val White         = Color(0xFFFFFFFF)
val TextMuted     = Color(0xFFAA9FCC)

val GradButton    = Brush.horizontalGradient(listOf(Color(0xFF7B2FFF), Color(0xFFCC3DF4)))
val GradButtonDim = Brush.horizontalGradient(listOf(Color(0xFF3A1A70), Color(0xFF5A1A80)))
val GradPrizeCard = Brush.horizontalGradient(listOf(Color(0xFF2A1848), Color(0xFF3D2800)))
val GradSearch = Brush.horizontalGradient(listOf(Color(0xFF7B2FFF), Color(0xFF9B59FC)))
val GradJoinBtn = Brush.horizontalGradient(listOf(Color(0xFF06C8C8), Color(0xFF9B59FC)))
val GradAvatar = Brush.linearGradient(
    colors = listOf(Color(0xFFFFB703), Color(0xFFE07B00)),
    start = Offset(0f, 0f),
    end = Offset(200f, 200f)
)
val GradAchievementUnlocked = Brush.linearGradient(
    colors = listOf(Color(0xFF6B3A00), Color(0xFF4A2800)),
    start = Offset(0f, 0f),
    end = Offset(0f, 200f)
)
val GradAchievementLocked = Brush.linearGradient(
    colors = listOf(Color(0xFF2A1F45), Color(0xFF1E1535)),
    start = Offset(0f, 0f),
    end = Offset(0f, 200f)
)
val GradCreate = Brush.horizontalGradient(listOf(Color(0xFFCC3DF4), Color(0xFFFF6B9D)))
val GradJoin = Brush.horizontalGradient(listOf(Color(0xFF5B5BFF), Color(0xFF00D4FF)))
val GradCoin = Brush.linearGradient(
    colors = listOf(Color(0xFFFFB703), Color(0xFFE07B00)),
    start = Offset(0f, 0f),
    end = Offset(500f, 300f)
)
