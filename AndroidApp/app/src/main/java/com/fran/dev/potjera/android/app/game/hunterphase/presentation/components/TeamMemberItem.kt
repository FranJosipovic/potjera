package com.fran.dev.potjera.android.app.game.hunterphase.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.state.PlayersAnsweringPlayer
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.Cyan
import com.fran.dev.potjera.android.app.ui.theme.White

//region ── Player avatar in the players row ───────────────────────────────────
@Composable
fun TeamMemberItem(
    member: PlayersAnsweringPlayer,
    isCaptain: Boolean,
) {
    val borderColor = if (isCaptain) Cyan else BgCardBorder
    val bgColor = if (isCaptain) Cyan.copy(alpha = 0.1f) else BgDeep

    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.Companion
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Companion.Center
        ) {
            Text(member.emoji, fontSize = 22.sp)
        }
        Text(
            member.name.take(10),
            color = White,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Companion.Ellipsis,
            textAlign = TextAlign.Companion.Center
        )
        if (isCaptain) {
            Text("👑 Captain", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Companion.Bold)
        }
    }
}