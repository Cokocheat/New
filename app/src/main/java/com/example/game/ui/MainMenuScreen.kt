package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.*
import com.example.game.viewmodel.AppScreen
import com.example.game.viewmodel.GameViewModel
import kotlin.math.*

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val playerName by viewModel.playerName.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val selectedSkin by viewModel.selectedSkin.collectAsState()
    val selectedHat by viewModel.selectedHat.collectAsState()
    val selectedFace by viewModel.selectedFace.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()

    val claimedDays by viewModel.claimedDays.collectAsState()
    val loginDay by viewModel.loginDay.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var showRoomDialog by remember { mutableStateOf(false) }
    var showDailyRewardDialog by remember { mutableStateOf(!viewModel.isDayClaimed(loginDay)) }

    // Pulsing button animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnPulse"
    )

    // Animated worm slithering preview timer
    val wormAnimProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wormPreview"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C101A), Color(0xFF13192B), Color(0xFF090D17))
                )
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        // TOP HEADER BAR: Coins, High Score, Room, Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Profile badge with editable name
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD111828))
                    .border(1.dp, Color(0x334A6999), RoundedCornerShape(20.dp))
                    .clickable { showNameDialog = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(selectedSkin.colors.first(), Color(0xFF10141E))
                            )
                        )
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ubah Nama",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "REKOR: $highScore",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Room Selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD111828))
                    .border(1.dp, Color(0x3300E676), RoundedCornerShape(20.dp))
                    .clickable { showRoomDialog = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedRoom.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedRoom.region} • ${selectedRoom.pingMs}ms",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            // Right: 7-Day Rewards, Coins balance & Settings Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 7-Day Login Reward Button
                Button(
                    onClick = { showDailyRewardDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("daily_reward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HADIAH 7 HARI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xDD111828))
                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$coins",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD111828))
                        .border(1.dp, Color(0x334A6999), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // CENTER: Game Title, Slithering Worm Preview, and Big Play Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 40.dp, bottom = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left: Title & Features
            Column(
                modifier = Modifier.width(320.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "NANA TAYANTUH",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "EXPANSIVE ARENA 11,200M • TACTICAL RADAR",
                    fontSize = 12.sp,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Jelajahi peta raksasa 11.200 unit, gunakan radar taktis untuk melacak musuh, makan makanan lezat, dan kuasai leaderboard!",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Play Button
                Button(
                    onClick = { viewModel.startGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .height(60.dp)
                        .shadow(18.dp, RoundedCornerShape(24.dp))
                        .testTag("play_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN SEKARANG",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            // Center / Right: Slithering Worm Preview
            Box(
                modifier = Modifier
                    .size(260.dp, 220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x33101626))
                    .border(2.dp, Color(0x333F51B5), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                WormSlitherPreview(
                    skin = selectedSkin,
                    hat = selectedHat,
                    face = selectedFace,
                    animAngle = wormAnimProgress
                )
            }
        }

        // BOTTOM BAR: Wardrobe, Leaderboard, Server
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.setScreen(AppScreen.SHOP) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("shop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kostum & Skin", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.setScreen(AppScreen.LEADERBOARD) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("leaderboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leaderboard Global", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            }
        }

        // Dialog: Edit Name
        if (showNameDialog) {
            EditNameDialog(
                currentName = playerName,
                onSave = {
                    viewModel.setPlayerName(it)
                    showNameDialog = false
                },
                onDismiss = { showNameDialog = false }
            )
        }

        // Dialog: Choose Room
        if (showRoomDialog) {
            ChooseRoomDialog(
                selectedRoom = selectedRoom,
                onSelect = {
                    viewModel.selectRoom(it)
                    showRoomDialog = false
                },
                onDismiss = { showRoomDialog = false }
            )
        }

        // Dialog: 7-Day Login Rewards
        if (showDailyRewardDialog) {
            DailyRewardDialog(
                viewModel = viewModel,
                onDismiss = { showDailyRewardDialog = false }
            )
        }
    }
}

@Composable
private fun WormSlitherPreview(
    skin: Skin,
    hat: Hat,
    face: Face,
    animAngle: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radiusX = size.width * 0.32f
        val radiusY = size.height * 0.28f

        val segCount = 22
        val segRadius = 16f
        val colors = skin.colors

        for (i in segCount downTo 0) {
            val offsetAngle = animAngle - (i * 0.16f)
            val sx = center.x + cos(offsetAngle) * radiusX
            val sy = center.y + sin(offsetAngle * 2f) * (radiusY * 0.85f)
            val pos = Offset(sx, sy)

            val color = when (skin.pattern) {
                PatternType.STRIPES -> colors[(i / 3) % colors.size]
                PatternType.RAINBOW -> colors[(i / 2) % colors.size]
                PatternType.NEON_GLOW -> colors[i % colors.size]
                PatternType.POLKA_DOTS -> if (i % 3 == 0) colors.last() else colors.first()
                else -> colors.first()
            }

            drawCircle(color = Color(0xFF141414), radius = segRadius + 2f, center = pos)
            drawCircle(color = color, radius = segRadius, center = pos)
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = segRadius * 0.5f, center = pos - Offset(segRadius * 0.25f, segRadius * 0.25f))
        }

        // Head
        val hx = center.x + cos(animAngle) * radiusX
        val hy = center.y + sin(animAngle * 2f) * (radiusY * 0.85f)
        val headPos = Offset(hx, hy)
        val headRadius = segRadius * 1.3f

        drawCircle(color = Color(0xFF141414), radius = headRadius + 2.5f, center = headPos)
        drawCircle(color = colors.first(), radius = headRadius, center = headPos)
        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = headRadius * 0.5f, center = headPos - Offset(headRadius * 0.25f, headRadius * 0.25f))

        // Eyes
        val eyeRadius = headRadius * 0.42f
        val pupilRadius = eyeRadius * 0.52f
        val leftEye = headPos + Offset(-headRadius * 0.35f, -headRadius * 0.25f)
        val rightEye = headPos + Offset(headRadius * 0.35f, -headRadius * 0.25f)

        drawCircle(color = Color(0xFF141414), radius = eyeRadius + 1.5f, center = leftEye)
        drawCircle(color = Color(0xFF141414), radius = eyeRadius + 1.5f, center = rightEye)
        drawCircle(color = Color.White, radius = eyeRadius, center = leftEye)
        drawCircle(color = Color.White, radius = eyeRadius, center = rightEye)
        drawCircle(color = Color(0xFF141414), radius = pupilRadius, center = leftEye)
        drawCircle(color = Color(0xFF141414), radius = pupilRadius, center = rightEye)
        drawCircle(color = Color.White, radius = pupilRadius * 0.45f, center = leftEye - Offset(2f, 2f))
        drawCircle(color = Color.White, radius = pupilRadius * 0.45f, center = rightEye - Offset(2f, 2f))

        // Blushing cheeks
        drawCircle(color = Color(0x66FF4081), radius = 4.5f, center = headPos + Offset(-headRadius * 0.48f, headRadius * 0.2f))
        drawCircle(color = Color(0x66FF4081), radius = 4.5f, center = headPos + Offset(headRadius * 0.48f, headRadius * 0.2f))

        // Mouth
        if (face.type == FaceType.OPEN_MOUTH) {
            drawCircle(color = Color(0xFF880E4F), radius = headRadius * 0.32f, center = headPos + Offset(0f, headRadius * 0.38f))
            drawCircle(color = Color(0xFFFF4081), radius = headRadius * 0.18f, center = headPos + Offset(0f, headRadius * 0.48f))
        } else {
            drawArc(
                color = Color(0xFF1E1E24),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = headPos - Offset(headRadius * 0.35f, -headRadius * 0.05f),
                size = androidx.compose.ui.geometry.Size(headRadius * 0.7f, headRadius * 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }

        // Hat
        val hatCenter = headPos - Offset(0f, headRadius * 0.95f)
        when (hat.type) {
            HatType.CROWN -> {
                drawCircle(color = Color(0xFFFFD700), radius = 9f, center = hatCenter)
            }
            HatType.NINJA_HEADBAND -> {
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = headPos - Offset(headRadius * 0.5f, headRadius * 0.5f),
                    size = androidx.compose.ui.geometry.Size(headRadius * 1.0f, headRadius * 0.25f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
            HatType.WIZARD_HAT -> {
                drawCircle(color = Color(0xFF311B92), radius = 10f, center = hatCenter)
                drawCircle(color = Color(0xFFFFD700), radius = 3.5f, center = hatCenter)
            }
            HatType.DEVIL_HORNS -> {
                drawCircle(color = Color(0xFFFF1744), radius = 4.5f, center = hatCenter - Offset(headRadius * 0.35f, 0f))
                drawCircle(color = Color(0xFFFF1744), radius = 4.5f, center = hatCenter + Offset(headRadius * 0.35f, 0f))
            }
            HatType.PARTY_HAT -> {
                drawCircle(color = Color(0xFFFF4081), radius = 8f, center = hatCenter)
            }
            HatType.CAT_EARS -> {
                drawCircle(color = Color(0xFF212121), radius = 5f, center = hatCenter - Offset(headRadius * 0.35f, 0f))
                drawCircle(color = Color(0xFF212121), radius = 5f, center = hatCenter + Offset(headRadius * 0.35f, 0f))
            }
            HatType.ANGEL_HALO -> {
                drawOval(
                    color = Color(0xFFFFD700),
                    topLeft = hatCenter - Offset(headRadius * 0.45f, headRadius * 0.15f),
                    size = androidx.compose.ui.geometry.Size(headRadius * 0.9f, headRadius * 0.3f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ganti Nama Pemain",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 15) text = it },
                    label = { Text("Nama Cacing") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", color = Color.White)
                    }

                    Button(
                        onClick = { onSave(text.ifBlank { "Nana" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseRoomDialog(
    selectedRoom: GameRoom,
    onSelect: (GameRoom) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
            modifier = Modifier
                .width(420.dp)
                .border(2.dp, Color(0xFF3F51B5), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PILIH SERVER ROOM",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                Catalog.ROOMS.forEach { room ->
                    val isCurrent = room.id == selectedRoom.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) Color(0x3300E676) else Color(0xFF1C253B))
                            .border(
                                1.5.dp,
                                if (isCurrent) Color(0xFF00E676) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(room) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = room.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF651FFF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = room.badge,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${room.region} • ${room.playersCount}/${room.maxPlayers} Pemain",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "${room.pingMs} ms",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DailyRewardDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val loginDay by viewModel.loginDay.collectAsState()
    val claimedDays by viewModel.claimedDays.collectAsState()
    val currentReward = Catalog.DAILY_REWARDS.find { it.day == loginDay }
    val isTodayClaimed = viewModel.isDayClaimed(loginDay)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111728)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 660.dp)
                .border(2.dp, Color(0xFFFF9100), RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HADIAH LOGIN 7 HARI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
                    )
                }
                Text(
                    text = "Klaim koin dan skin eksklusif setiap hari untuk memperkuat cacingmu!",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7 Days Cards Row
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(Catalog.DAILY_REWARDS) { reward ->
                        val isClaimed = viewModel.isDayClaimed(reward.day)
                        val isCurrentDay = reward.day == loginDay
                        val isLocked = reward.day > loginDay

                        val cardBg = when {
                            isClaimed -> Color(0xFF17281F)
                            isCurrentDay && !isClaimed -> Color(0xFF33200B)
                            else -> Color(0xFF171E2D)
                        }
                        val cardBorder = when {
                            isClaimed -> Color(0xFF00E676)
                            isCurrentDay && !isClaimed -> Color(0xFFFF9100)
                            else -> Color(0x33FFFFFF)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            modifier = Modifier
                                .width(94.dp)
                                .height(130.dp)
                                .border(1.5.dp, cardBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = reward.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrentDay) Color(0xFFFFD700) else Color.White
                                )

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isClaimed) Color(0x3300E676) else Color(0x33FFD700)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when {
                                        reward.skinId != null -> Icons.Default.Palette
                                        reward.hatId != null -> Icons.Default.Diamond
                                        else -> Icons.Default.MonetizationOn
                                    }
                                    val tint = when {
                                        isClaimed -> Color(0xFF00E676)
                                        reward.skinId != null -> Color(0xFF00E5FF)
                                        reward.hatId != null -> Color(0xFFFF4081)
                                        else -> Color(0xFFFFD700)
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = "+${reward.coins}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )

                                when {
                                    isClaimed -> {
                                        Text(
                                            text = "DIKLAIM ✓",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676)
                                        )
                                    }
                                    isCurrentDay -> {
                                        Text(
                                            text = "HARI INI!",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFF9100)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Terkunci",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Claim Action Button
                if (!isTodayClaimed && currentReward != null) {
                    Button(
                        onClick = {
                            viewModel.claimDailyReward(currentReward)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("claim_daily_reward_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KLAIM HADIAH HARI KE-$loginDay SEKARANG!",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Hadiah Hari Ini Sudah Diklaim - Kembali Besok!",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

