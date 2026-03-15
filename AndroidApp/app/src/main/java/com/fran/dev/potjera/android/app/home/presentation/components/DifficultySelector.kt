package com.fran.dev.potjera.android.app.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Difficulty {
    EASY, MEDIUM, HARD
}

private data class DifficultyOption(
    val difficulty: Difficulty,
    val emoji: String,
    val label: String,
    val subtitle: String,
    val reward: String,
    val bgColor: Color,
    val borderColor: Color,
    val iconBg: Color,
)

private val options = listOf(
    DifficultyOption(
        difficulty = Difficulty.EASY,
        emoji = "🧠",
        label = "Lako",
        subtitle = "Lovac 50% točnosti",
        reward = "+500 novčića po pobjedi",
        bgColor = Color(0xFF0D3B2E),
        borderColor = Color(0xFF1DB954),
        iconBg = Color(0xFF1DB954),
    ),
    DifficultyOption(
        difficulty = Difficulty.MEDIUM,
        emoji = "🔥",
        label = "Srednje",
        subtitle = "Lovac 75% točnosti",
        reward = "+1000 novčića po pobjedi",
        bgColor = Color(0xFF2E1A0A),
        borderColor = Color(0xFFE07A10),
        iconBg = Color(0xFFE07A10),
    ),
    DifficultyOption(
        difficulty = Difficulty.HARD,
        emoji = "💀",
        label = "Teško",
        subtitle = "Lovac 90% točnosti",
        reward = "+2000 novčića po pobjedi",
        bgColor = Color(0xFF2A0A14),
        borderColor = Color(0xFFB01030),
        iconBg = Color(0xFFB01030),
    ),
)

@Composable
fun DifficultySelector(
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            DifficultyCard(option = option, onClick = { onSelect(option.difficulty) })
        }
    }
}

@Composable
private fun DifficultyCard(
    option: DifficultyOption,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(option.bgColor)
            .border(1.5.dp, option.borderColor, RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(option.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(option.emoji, fontSize = 22.sp)
        }

        // Text block
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = option.subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = option.reward,
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Arrow
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = option.borderColor,
            modifier = Modifier.size(20.dp)
        )
    }
}