package com.fran.dev.potjera.android.app.room.presentation.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Colors
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF1A1035)
private val BgCard = Color(0xFF251848)
private val BgCardBorder = Color(0xFF3A2A6A)
private val BgInput = Color(0xFF1A1035)
private val Gold = Color(0xFFF5A623)
private val Purple = Color(0xFF9B59FC)
private val Green = Color(0xFF2ECC71)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFFAA9FCC)
private val GradJoinBtn = Brush.horizontalGradient(listOf(Color(0xFF5B5BFF), Color(0xFF00D4FF)))
private val GradSearch = Brush.linearGradient(
    colors = listOf(Color(0xFF7B2FFF), Color(0xFFCC3DF4)),
    start = Offset(0f, 0f),
    end = Offset(0f, 100f)
)

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class PublicRoom(
    val id: String,
    val name: String,
    val hostUsername: String,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val entryFee: Int,
    val isOpen: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// JoinRoomScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun JoinRoomScreen(
    publicRooms: List<PublicRoom>,
    onBack: () -> Unit = {},
    onJoinById: (roomId: String) -> Unit = {},
    onJoinByCode: (code: String) -> Unit = {},
) {
    var roomCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed top section
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar
                JoinRoomTopBar(onBack = onBack)

                // Code input
                RoomCodeInput(
                    code = roomCode,
                    onChange = { roomCode = it.uppercase() },
                    onSearch = { if (roomCode.length == 6) onJoinByCode(roomCode) }
                )

                // Section title
                Text(
                    text = "Available Rooms",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scrollable room list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(publicRooms) { room ->
                    PublicRoomCard(
                        room = room,
                        onJoin = { onJoinById(room.id) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JoinRoomTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Join Room",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Room code input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomCodeInput(
    code: String,
    onChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Enter Room Code", color = TextMuted, fontSize = 13.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgInput)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                if (code.isEmpty()) {
                    Text(
                        text = "ROOM CODE...",
                        color = TextMuted.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
                BasicTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) onChange(it) },
                    textStyle = TextStyle(
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    cursorBrush = SolidColor(Purple),
                    singleLine = true
                )
            }

            // Search button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GradSearch)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSearch
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Public room card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PublicRoomCard(room: PublicRoom, onJoin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Room name + host
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = room.name,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Host: ${room.hostUsername}",
                color = TextMuted,
                fontSize = 13.sp
            )
        }

        // Players + fee + status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Players
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👥 ", fontSize = 13.sp)
                    Text(
                        text = "${room.currentPlayers}/${room.maxPlayers}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Entry fee
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙 ", fontSize = 13.sp)
                    Text(
                        text = room.entryFee.toString(),
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Open/Full badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (room.isOpen) Green.copy(alpha = 0.15f)
                        else Color.Red.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (room.isOpen) Green.copy(alpha = 0.5f)
                        else Color.Red.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (room.isOpen) "Open" else "Full",
                    color = if (room.isOpen) Green else Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Join button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (room.isOpen) GradJoinBtn else Brush.horizontalGradient(
                        listOf(
                            BgCardBorder,
                            BgCardBorder
                        )
                    )
                )
                .clickable(
                    enabled = room.isOpen,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onJoin
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = if (room.isOpen) White else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Join Room",
                    color = if (room.isOpen) White else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun JoinRoomScreenPreview() {
    val rooms = listOf(
        PublicRoom("1", "Quick Game", "Player_456", 2, 4, 100, true),
        PublicRoom("2", "High Stakes Challenge", "ProGamer99", 3, 4, 1000, true),
        PublicRoom("3", "Beginner Friendly", "Newbie_12", 1, 4, 100, true),
        PublicRoom("4", "Champions Only", "Legend_99", 4, 4, 500, false),
    )
    JoinRoomScreen(publicRooms = rooms)
}