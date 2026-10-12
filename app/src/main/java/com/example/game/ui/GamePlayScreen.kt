package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameWorld
import com.example.game.model.PotionType
import com.example.game.viewmodel.AppScreen
import com.example.game.viewmodel.GameViewModel
import kotlin.math.*

@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val world = viewModel.gameWorld
    val renderer = remember { GameRenderer() }

    BackHandler {
        if (!world.isPaused) {
            viewModel.togglePause()
        } else {
            viewModel.setScreen(AppScreen.MAIN_MENU)
        }
    }

    // Touch control state
    var joystickCenter by remember { mutableStateOf<Offset?>(null) }
    var joystickThumb by remember { mutableStateOf<Offset?>(null) }
    val joystickMaxRadius = 80f

    // Radar expanded state
    var isRadarExpanded by remember { mutableStateOf(false) }

    // Screen dimension state
    var screenWidth by remember { mutableFloatStateOf(1920f) }
    var screenHeight by remember { mutableFloatStateOf(1080f) }

    // Live frame ticker
    var frameTick by remember { mutableLongStateOf(0L) }

    // Observe game over
    LaunchedEffect(world.isGameOver) {
        if (world.isGameOver) {
            viewModel.onGameOver()
        }
    }

    // Smooth 120 FPS High-Performance Game Loop with dynamic delta-time
    LaunchedEffect(world.isGameOver, world.isPaused) {
        var lastNanos = 0L
        while (!world.isGameOver) {
            withFrameNanos { now ->
                if (lastNanos > 0L && !world.isPaused) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.003f, 0.033f)
                    world.update(dt)
                    frameTick++
                }
                lastNanos = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D17))
            .pointerInput(Unit) {
                // Immediate, responsive touch & drag steering
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // If touch is on bottom-right boost button or radar area, let UI controls handle it
                    if (down.position.x > size.width - 320f && down.position.y > size.height - 240f) {
                        return@awaitEachGesture
                    }

                    val touchPos = down.position
                    val screenCenter = Offset(size.width / 2f, size.height / 2f)

                    if (touchPos.x < size.width * 0.50f) {
                        joystickCenter = touchPos
                        joystickThumb = touchPos
                    } else {
                        // Direct touch steer towards touch location relative to center
                        val diff = touchPos - screenCenter
                        world.playerWorm.steerAngle(atan2(diff.y, diff.x))
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            val curPos = change.position
                            val center = joystickCenter
                            if (center != null) {
                                val diff = curPos - center
                                val dist = hypot(diff.x, diff.y)
                                val angle = atan2(diff.y, diff.x)
                                val clampedDist = min(dist, joystickMaxRadius)
                                joystickThumb = Offset(
                                    center.x + cos(angle) * clampedDist,
                                    center.y + sin(angle) * clampedDist
                                )
                                if (dist > 8f) {
                                    world.playerWorm.steerAngle(angle)
                                }
                            } else {
                                val diff = curPos - screenCenter
                                world.playerWorm.steerAngle(atan2(diff.y, diff.x))
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    joystickCenter = null
                    joystickThumb = null
                }
            }
    ) {
        // Main Game Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidth = size.width
            screenHeight = size.height

            if (frameTick >= 0L) {
                renderer.drawWorld(this, world, size.width, size.height)
            }

            // Draw Virtual Touch Joystick
            val center = joystickCenter
            val thumb = joystickThumb
            if (center != null && thumb != null) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = joystickMaxRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    radius = joystickMaxRadius,
                    center = center,
                    style = Stroke(width = 3.5f)
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                    radius = 30f,
                    center = thumb
                )
            }
        }

        // HUD - Top Left: Player Status Bar
        TopLeftHud(world = world, modifier = Modifier.align(Alignment.TopStart))

        // HUD - Top Center: Room Info & Pause Button
        TopCenterHud(
            world = world,
            roomName = viewModel.selectedRoom.collectAsState().value.name,
            onPauseClick = { viewModel.togglePause() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // HUD - Top Right: Live Room Leaderboard
        TopRightLeaderboard(world = world, frameTick = frameTick, modifier = Modifier.align(Alignment.TopEnd))

        // HUD - Top Center: Kill Notification Banner
        val killNotice = world.activeKillNotice
        AnimatedVisibility(
            visible = killNotice != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            if (killNotice != null) {
                KillNotificationBanner(notice = killNotice)
            }
        }

        // HUD - Bottom Right: Dedicated Modern Tactical Radar Minimap
        TacticalRadarOverlay(
            world = world,
            frameTick = frameTick,
            isExpanded = isRadarExpanded,
            onToggleExpand = { isRadarExpanded = !isRadarExpanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        )

        // HUD - Bottom Right: Ergonomic Speed Boost (TURBO) Button
        BoostButton(
            isBoosting = world.playerWorm.isBoosting,
            onBoostChanged = { boosting ->
                world.playerWorm.isBoosting = boosting
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = if (isRadarExpanded) 126.dp else 96.dp, bottom = 16.dp)
        )

        // Pause Modal Dialog
        if (world.isPaused && !world.isGameOver) {
            PauseDialog(
                viewModel = viewModel,
                onResume = { viewModel.togglePause() },
                onQuit = { viewModel.setScreen(AppScreen.MAIN_MENU) }
            )
        }

        // Revive with Coins Dialog (5-second countdown to continue)
        if (world.isRevivePromptActive && !world.isGameOver) {
            ReviveModal(
                world = world,
                viewModel = viewModel,
                onRevive = {
                    val canPay = viewModel.payRevive(50)
                    if (canPay) {
                        world.revivePlayer()
                    }
                },
                onDecline = {
                    world.declineRevive()
                }
            )
        }

        // Game Over Dialog
        if (world.isGameOver && !world.isRevivePromptActive) {
            GameOverModal(
                world = world,
                viewModel = viewModel,
                onReplay = {
                    viewModel.replayGame()
                },
                onExit = {
                    viewModel.setScreen(AppScreen.MAIN_MENU)
                }
            )
        }
    }
}

// MODERN COMPACT TACTICAL RADAR MINIMAP (Real-Time 120 FPS High-Precision Slither HUD)
@Composable
private fun TacticalRadarOverlay(
    world: GameWorld,
    frameTick: Long,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diameter = if (isExpanded) 102.dp else 74.dp
    val player = world.playerWorm

    Box(
        modifier = modifier
            .size(diameter)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xEE070D18))
            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), CircleShape)
            .clickable { onToggleExpand() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tick = frameTick
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val usableRadius = radius * 0.86f

            // Radar Distance Rings
            drawCircle(color = Color(0x2200E5FF), radius = usableRadius * 0.5f, center = center, style = Stroke(width = 0.8f))
            drawCircle(color = Color(0x3300E5FF), radius = usableRadius, center = center, style = Stroke(width = 1.0f))

            // Crosshair
            drawLine(color = Color(0x2200E5FF), start = Offset(center.x - usableRadius, center.y), end = Offset(center.x + usableRadius, center.y), strokeWidth = 0.8f)
            drawLine(color = Color(0x2200E5FF), start = Offset(center.x, center.y - usableRadius), end = Offset(center.x, center.y + usableRadius), strokeWidth = 0.8f)

            // Rotating 360° Radar Scanner Beam
            val scanAngle = world.radarScanAngle
            val beamEnd = center + Offset(cos(scanAngle) * usableRadius, sin(scanAngle) * usableRadius)
            drawLine(
                color = Color(0x9900E676),
                start = center,
                end = beamEnd,
                strokeWidth = 1.5f
            )

            if (!isExpanded) {
                // SECTOR RADAR MODE: Centered on Player (Sensor range 2000 units)
                // Dynamically detects every nearby enemy worm and movement!
                val sensorRange = 2000f
                val pPos = player.headPosition

                // Nearby Enemy Worms
                for (bot in world.botWorms) {
                    if (!bot.isAlive || bot.isDying) continue
                    val relX = bot.headPosition.x - pPos.x
                    val relY = bot.headPosition.y - pPos.y
                    val dist = hypot(relX, relY)

                    if (dist <= sensorRange) {
                        val dotNormX = relX / sensorRange
                        val dotNormY = relY / sensorRange
                        val dotPos = center + Offset(dotNormX * usableRadius, dotNormY * usableRadius)

                        // Glowing pulsing red enemy blip
                        drawCircle(color = Color(0x66FF1744), radius = 3.6f, center = dotPos)
                        drawCircle(color = Color(0xFFFF1744), radius = 2.0f, center = dotPos)
                    } else if (dist <= sensorRange * 1.8f) {
                        // Perimeter threat pointer
                        val angle = atan2(relY, relX)
                        val pPosEdge = center + Offset(cos(angle) * (usableRadius - 2f), sin(angle) * (usableRadius - 2f))
                        drawCircle(color = Color(0xFFFF9100), radius = 1.5f, center = pPosEdge)
                    }
                }

                // Player at Center with real-time heading needle
                if (player.isAlive) {
                    val pulse = (sin(tick * 0.16f) + 1f) * 0.5f
                    drawCircle(color = Color(0x5500E5FF), radius = 4.5f + pulse * 2.5f, center = center)
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.8f, center = center)

                    val pAngle = player.currentAngle
                    val arrLen = 6f
                    val tip = center + Offset(cos(pAngle) * arrLen, sin(pAngle) * arrLen)
                    drawLine(color = Color(0xFF00E676), start = center, end = tip, strokeWidth = 2.0f)
                }
            } else {
                // GLOBAL ARENA OVERVIEW MODE: Entire 11KM map
                val worldRadius = world.arenaRadius

                // Enemy Bots
                for (bot in world.botWorms) {
                    if (!bot.isAlive || bot.isDying) continue
                    val normX = (bot.headPosition.x / worldRadius).coerceIn(-1f, 1f)
                    val normY = (bot.headPosition.y / worldRadius).coerceIn(-1f, 1f)
                    val dotPos = center + Offset(normX * usableRadius, normY * usableRadius)
                    drawCircle(color = Color(0xFFFF1744), radius = 2.2f, center = dotPos)
                }

                // Player Beacon
                if (player.isAlive) {
                    val normX = (player.headPosition.x / worldRadius).coerceIn(-1f, 1f)
                    val normY = (player.headPosition.y / worldRadius).coerceIn(-1f, 1f)
                    val pCenter = center + Offset(normX * usableRadius, normY * usableRadius)
                    drawCircle(color = Color(0x6600E5FF), radius = 5.5f, center = pCenter)
                    drawCircle(color = Color(0xFF00E5FF), radius = 2.8f, center = pCenter)
                }
            }
        }

        // Circular Radar Mode Tag
        Text(
            text = if (isExpanded) "GLOBAL" else "SECTOR",
            color = Color(0xFF00E5FF).copy(alpha = 0.85f),
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        )
    }
}

// MODERN TOP-LEFT PLAYER HUD (Compact & Non-Intrusive)
@Composable
private fun TopLeftHud(world: GameWorld, modifier: Modifier = Modifier) {
    val player = world.playerWorm

    Column(
        modifier = modifier
            .padding(8.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(Color(0xDD0E1526), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x334A6999), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Player Avatar with metallic level border
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(player.skin.colors.first(), Color(0xFF10141E))
                        )
                    )
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = player.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SKOR: ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${player.score}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Compact stats pills with Material Icons (NO EMOJIS)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kills pill
            Row(
                modifier = Modifier
                    .background(Color(0x33FF5252), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${world.sessionKills}",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }

            // Coins pill
            Row(
                modifier = Modifier
                    .background(Color(0x33FFD700), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "+${world.coinsCollected}",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }

            // Length pill
            Row(
                modifier = Modifier
                    .background(Color(0x3300E676), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LinearScale,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${player.segments.size}m",
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // Active Potions / Shield Badges
        if (player.hasShield || player.activePotions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (player.spawnShieldTimer > 0f) {
                    Row(
                        modifier = Modifier
                            .background(Color(0x3300E5FF), RoundedCornerShape(6.dp))
                            .border(0.8.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "SHIELD ${player.spawnShieldTimer.toInt() + 1}s",
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                player.activePotions.values.forEach { potion ->
                    val color = Color(potion.type.colorHex)
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .border(0.8.dp, color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${potion.type.displayName} ${potion.remainingSeconds.toInt()}s",
                            color = color,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// TOP-CENTER ROOM INFO & PAUSE
@Composable
private fun TopCenterHud(
    world: GameWorld,
    roomName: String,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 12.dp)
            .background(Color(0xCC0E1526), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x2800E5FF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E676))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = roomName,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(14.dp))
        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(28.dp)
                .testTag("pause_button")
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Jeda Game",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// TOP-RIGHT LEADERBOARD (Compact & Live Updating)
@Composable
private fun TopRightLeaderboard(world: GameWorld, frameTick: Long, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    // Live update triggered every 12 frames (smooth 120 FPS reaction to kills & eating)
    val entries = remember(frameTick / 12L) {
        world.getLiveLeaderboard()
    }

    Column(
        modifier = modifier
            .padding(8.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(Color(0xDD0E1526), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x334A6999), RoundedCornerShape(14.dp))
            .padding(6.dp)
            .widthIn(min = 132.dp, max = 152.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TOP ROOM",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            entries.take(5).forEach { item ->
                val isPlayer = item.isPlayer
                val bgItemColor = if (isPlayer) Color(0x3300E5FF) else Color.Transparent
                val textColor = if (isPlayer) Color(0xFF00E5FF) else when (item.rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFE0E0E0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.White.copy(alpha = 0.85f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgItemColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp, vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${item.rank}",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.width(18.dp)
                        )
                        // Country code badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF222F47), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = item.countryCode,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.name.take(7),
                            color = textColor,
                            fontWeight = if (isPlayer) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = "${item.score}",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// MODERN TURBO BOOST BUTTON
@Composable
private fun BoostButton(
    isBoosting: Boolean,
    onBoostChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isBoosting) listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
                    else listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
                )
            )
            .border(
                width = 3.dp,
                color = if (isBoosting) Color(0xFFFFEA00) else Color.White.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onBoostChanged(true)
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onBoostChanged(false)
                }
            }
            .testTag("boost_button"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Kecepatan Turbo Boost",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "TURBO",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// PAUSE DIALOG
@Composable
private fun PauseDialog(
    viewModel: GameViewModel,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
            modifier = Modifier
                .width(360.dp)
                .border(2.dp, Color(0xFF3F51B5), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME DIJEDA",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Text("Lanjutkan Main", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.toggleSound() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = if (viewModel.soundEnabled.collectAsState().value) "Suara: AKTIF" else "Suara: MATI",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onQuit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("exit_game_button")
                ) {
                    Text("Keluar ke Menu", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// GAME OVER MODAL
@Composable
private fun GameOverModal(
    world: GameWorld,
    viewModel: GameViewModel,
    onReplay: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
            modifier = Modifier
                .width(420.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .border(3.dp, Color(0xFFFF1744), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1744)
                )
                Text(
                    text = "Cacingmu bertabrakan dengan lawan!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Summary Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B2338), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Skor Akhir", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(
                            text = "${world.playerWorm.score}",
                            color = Color(0xFFFFD700),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kills", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(
                            text = "${world.sessionKills}",
                            color = Color(0xFFFF5252),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Koin Didapat", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(
                            text = "+${world.coinsCollected}",
                            color = Color(0xFF00E676),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("gameover_menu_button")
                    ) {
                        Text("Menu Utama", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReplay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("gameover_replay_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Main Lagi", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// IN-GAME KILL NOTIFICATION BANNER
@Composable
private fun KillNotificationBanner(notice: com.example.game.model.KillNotice) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE160B0F)),
        modifier = Modifier
            .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(20.dp))
            .shadow(16.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF1744)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MUSUH DIELIMINASI!",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0x3300E676), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "+${notice.bonusCoins} KOIN",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    text = "Kamu mengalahkan ${notice.victimName}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// REVIVE WITH COINS MODAL DIALOG
@Composable
private fun ReviveModal(
    world: GameWorld,
    viewModel: GameViewModel,
    onRevive: () -> Unit,
    onDecline: () -> Unit
) {
    val coins by viewModel.coins.collectAsState()
    val remainingSec = max(1, ceil(world.reviveCountdownSec).toInt())
    val canAfford = coins >= 50

    Dialog(onDismissRequest = onDecline) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13192B)),
            modifier = Modifier
                .width(360.dp)
                .shadow(24.dp, RoundedCornerShape(26.dp))
                .border(2.5.dp, Color(0xFF00E5FF), RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LANJUTKAN PERMAINAN?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bangkit kembali dengan perisai pelindung 5 detik!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Countdown Timer Circle
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B243B))
                        .border(3.5.dp, Color(0xFFFF9100), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$remainingSec",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9100)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Revive Button with Coins
                Button(
                    onClick = onRevive,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("revive_with_coins_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canAfford) "Lanjutkan (50 Koin)" else "Koin Kurang (Butuh 50)",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Decline / Give up Button
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("decline_revive_button")
                ) {
                    Text("Menyerah (Selesai)", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

