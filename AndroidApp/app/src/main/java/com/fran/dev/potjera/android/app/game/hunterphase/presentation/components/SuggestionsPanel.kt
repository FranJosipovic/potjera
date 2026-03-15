package com.fran.dev.potjera.android.app.game.hunterphase.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fran.dev.potjera.android.app.game.models.SuggestionItem
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgCardBorder
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White

@Composable
fun SuggestionsPanel(suggestions: List<SuggestionItem>) {
    Column(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(
                1.dp,
                Purple.copy(alpha = 0.4f),
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("💬", fontSize = 14.sp)
            Text(
                "TEAM SUGGESTIONS",
                color = Purple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Companion.Bold,
                letterSpacing = 1.5.sp
            )
        }
        suggestions.forEach { s ->
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(BgDeep)
                    .border(
                        1.dp,
                        BgCardBorder,
                        androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.Companion
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Purple.copy(alpha = 0.2f))
                        .border(1.dp, Purple.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Companion.Center
                ) { Text("👤", fontSize = 12.sp) }
                Column(modifier = Modifier.Companion.weight(1f)) {
                    Text(s.username, color = TextMuted, fontSize = 11.sp)
                    Text(
                        s.suggestion,
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Companion.SemiBold
                    )
                }
            }
        }
    }
}