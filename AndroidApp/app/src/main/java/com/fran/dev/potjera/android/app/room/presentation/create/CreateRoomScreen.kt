package com.fran.dev.potjera.android.app.room.presentation.create

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fran.dev.potjera.android.app.domain.models.user.User
import com.fran.dev.potjera.android.app.ui.components.LoadingButton
import com.fran.dev.potjera.android.app.ui.theme.*

private val entryFeeOptions = listOf(100, 250, 500, 1000)

@Composable
fun CreateRoomScreen(
    user: User,
    onBack: () -> Unit = {},
    onCreateRoom: (roomId: String) -> Unit = { _-> },
) {

    val createRoomViewModel = hiltViewModel<CreateRoomViewModel>()
    val isLoading by createRoomViewModel.isLoading.collectAsState()

    var isPrivate by remember { mutableStateOf(false) }
    var selectedFee by remember { mutableIntStateOf(1000) }

    val roomName = "${user.username}'s Room"
    val playerCount = 4
    val hunterCount = 1
    val questionsPerRound = "5-10"
    val prizePool = selectedFee * playerCount

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            CreateRoomTopBar(onBack = onBack)

            // Room config card
            RoomConfigCard(
                roomName = roomName,
                isPrivate = isPrivate,
                onPrivate = { isPrivate = it },
                selectedFee = selectedFee,
                onFeeSelect = { selectedFee = it },
                prizePool = prizePool
            )

            // Room setup summary card
            RoomSetupCard(
                playerCount = playerCount,
                hunterCount = hunterCount,
                questionsPerRound = questionsPerRound,
                prizePool = prizePool
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Create button

            LoadingButton(
                isLoading = isLoading,
                onClick = {
                    createRoomViewModel.createRoom(
                        isPrivate,
                        onRoomCreated = { roomId -> onCreateRoom(roomId) })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GradButton)
                    .padding(vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆  ", fontSize = 18.sp)
                    Text(
                        text = "Create Room",
                        color = White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }



            Text(
                text = "Room will be created and you'll be moved to the lobby",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CreateRoomTopBar(onBack: () -> Unit) {
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
            text = "Create Room",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Room config card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomConfigCard(
    roomName: String,
    isPrivate: Boolean,
    onPrivate: (Boolean) -> Unit,
    selectedFee: Int,
    onFeeSelect: (Int) -> Unit,
    prizePool: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Room name (read-only display)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Room Name", color = TextMuted, fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgInput)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = roomName,
                    color = TextMuted,
                    fontSize = 15.sp
                )
            }
        }

        // Private room toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A1060)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Private Room",
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Invite only with code", color = TextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = isPrivate,
                onCheckedChange = onPrivate,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = Purple,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = BgCardBorder
                )
            )
        }

        // Entry fee selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Entry Fee (Coins)", color = TextMuted, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entryFeeOptions.forEach { fee ->
                    val isSelected = fee == selectedFee
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Gold else BgInput
                            )
                            .border(
                                1.dp,
                                if (isSelected) Gold else BgCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onFeeSelect(fee) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fee.toString(),
                            color = if (isSelected) BgDeep else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Winner takes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 ", fontSize = 13.sp)
                Text(
                    text = "Winner takes $prizePool coins",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Room setup summary card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomSetupCard(
    playerCount: Int,
    hunterCount: Int,
    questionsPerRound: String,
    prizePool: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Room Setup",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = BgCardBorder)

        // Setup rows
        SetupRow(label = "Players:", value = "$playerCount Players", valueColor = White)
        SetupRow(label = "Hunter:", value = "$hunterCount Hunter", valueColor = White)
        SetupRow(label = "Questions per round:", value = questionsPerRound, valueColor = White)
        SetupRow(
            label = "Prize Pool:",
            value = "🪙 $prizePool",
            valueColor = Gold
        )
    }
}

@Composable
private fun SetupRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1035, widthDp = 360, heightDp = 780)
@Composable
fun CreateRoomScreenPreview() {
    val previewUser = User(
        id = 1,
        username = "Player_123",
        imageUrl = null,
        coins = 12_450,
        rank = 47,
        xp = 2450,
        gamesPlayed = 38,
        gamesWon = 24
    )
    CreateRoomScreen(user = previewUser)
}