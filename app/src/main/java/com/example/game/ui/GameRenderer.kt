package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.game.engine.GameWorld
import com.example.game.model.*
import kotlin.math.*

class GameRenderer {

    // Colors
    private val bgColor = Color(0xFF0C101A)
    private val gridLineColor = Color(0x18425C8F)
    private val boundaryColor = Color(0xFFFF1744)
    private val boundaryGlowColor = Color(0x44FF1744)

    // Smooth Camera Tracking (silky smooth lerp without micro-jitter)
    private var smoothCamX = 0f
    private var smoothCamY = 0f
    private var isCamInitialized = false

    fun drawWorld(
        scope: DrawScope,
        world: GameWorld,
        viewportWidth: Float,
        viewportHeight: Float
    ) {
        val player = world.playerWorm

        // Butter-smooth camera interpolation
        if (!isCamInitialized) {
            smoothCamX = player.headPosition.x
            smoothCamY = player.headPosition.y
            isCamInitialized = true
        } else {
            smoothCamX += (player.headPosition.x - smoothCamX) * 0.18f
            smoothCamY += (player.headPosition.y - smoothCamY) * 0.18f
        }

        val cameraX = smoothCamX
        val cameraY = smoothCamY

        // Dynamic Camera Zoom: gently zooms out as player grows in the vast arena
        val baseScale = (viewportHeight / 620f).coerceIn(0.65f, 1.30f)
        val growthFactor = (1f / (1f + log10(max(10f, player.score.toFloat())) * 0.10f))
        val zoom = (baseScale * growthFactor).coerceIn(0.38f, 1.30f)

        val halfW = viewportWidth / 2f
        val halfH = viewportHeight / 2f

        // World to Screen transformation helper
        fun toScreen(pos: Vector2D): Offset {
            return Offset(
                halfW + (pos.x - cameraX) * zoom,
                halfH + (pos.y - cameraY) * zoom
            )
        }

        // 1. Draw Arena Background & Grid
        scope.drawRect(color = bgColor, size = Size(viewportWidth, viewportHeight))

        // Grid lines (rendered in camera space)
        val gridSize = 140f * zoom
        val offsetX = (halfW - cameraX * zoom) % gridSize
        val offsetY = (halfH - cameraY * zoom) % gridSize

        val cols = ceil((viewportWidth + gridSize) / gridSize).toInt() + 2
        for (i in 0..cols) {
            val lx = offsetX - gridSize + i * gridSize
            scope.drawLine(
                color = gridLineColor,
                start = Offset(lx, 0f),
                end = Offset(lx, viewportHeight),
                strokeWidth = 1.2f
            )
        }

        val rows = ceil((viewportHeight + gridSize) / gridSize).toInt() + 2
        for (j in 0..rows) {
            val ly = offsetY - gridSize + j * gridSize
            scope.drawLine(
                color = gridLineColor,
                start = Offset(0f, ly),
                end = Offset(viewportWidth, ly),
                strokeWidth = 1.2f
            )
        }

        // 2. Arena Outer Boundary Barrier (Vast 5600f arena)
        val arenaCenterScreen = toScreen(Vector2D(0f, 0f))
        val arenaRadiusScreen = world.arenaRadius * zoom

        // Red outer boundary warning glow
        scope.drawCircle(
            color = boundaryGlowColor,
            radius = arenaRadiusScreen + 24f * zoom,
            center = arenaCenterScreen,
            style = Stroke(width = 42f * zoom)
        )
        scope.drawCircle(
            color = boundaryColor,
            radius = arenaRadiusScreen,
            center = arenaCenterScreen,
            style = Stroke(width = 10f * zoom)
        )

        // 3. Draw Foods (culling outside viewport for smooth 60fps performance)
        val screenMargin = 90f
        for (food in world.foods) {
            val screenPos = toScreen(food.position)
            if (screenPos.x < -screenMargin || screenPos.x > viewportWidth + screenMargin ||
                screenPos.y < -screenMargin || screenPos.y > viewportHeight + screenMargin
            ) {
                continue
            }

            drawFood(scope, food, screenPos, zoom)
        }

        // 4. Draw Bots (with smart off-screen culling)
        for (bot in world.botWorms) {
            if (bot.isAlive) {
                drawWorm(scope, bot, ::toScreen, zoom, viewportWidth, viewportHeight)
            }
        }

        // 5. Draw Player Worm
        if (player.isAlive) {
            drawWorm(scope, player, ::toScreen, zoom, viewportWidth, viewportHeight)
        }

        // 6. Draw Smooth Death Dissolve Ghost Segments
        drawDeathGhosts(scope, world, ::toScreen, zoom, viewportWidth, viewportHeight)

        // 7. Draw Smooth Death Particle Bursts
        drawDeathParticles(scope, world, ::toScreen, zoom, viewportWidth, viewportHeight)

        // 8. Draw Off-Screen Threat Radar Chevron Arrows
        drawOffscreenThreats(scope, world, ::toScreen, viewportWidth, viewportHeight)
    }

    private fun drawOffscreenThreats(
        scope: DrawScope,
        world: GameWorld,
        toScreen: (Vector2D) -> Offset,
        viewportW: Float,
        viewportH: Float
    ) {
        val player = world.playerWorm
        if (!player.isAlive) return

        val pHead = player.headPosition
        val edgePadding = 24f

        for (bot in world.botWorms) {
            if (!bot.isAlive) continue
            val bHead = bot.headPosition
            val dist = pHead.distanceTo(bHead)

            // Only indicate nearby threats within 1100 units
            if (dist < 1100f && dist > 160f) {
                val screenPos = toScreen(bHead)
                // If offscreen, draw directional threat chevron at screen border
                if (screenPos.x < 0 || screenPos.x > viewportW || screenPos.y < 0 || screenPos.y > viewportH) {
                    val dx = screenPos.x - viewportW / 2f
                    val dy = screenPos.y - viewportH / 2f
                    val angle = atan2(dy, dx)

                    // Project to screen edges
                    val halfW = (viewportW / 2f) - edgePadding
                    val halfH = (viewportH / 2f) - edgePadding

                    val scaleX = abs(halfW / cos(angle))
                    val scaleY = abs(halfH / sin(angle))
                    val scale = min(scaleX, scaleY)

                    val edgeX = (viewportW / 2f) + cos(angle) * scale
                    val edgeY = (viewportH / 2f) + sin(angle) * scale
                    val edgePos = Offset(edgeX, edgeY)

                    // Draw threat chevron arrow
                    val arrowColor = if (bot.score > player.score) Color(0xFFFF1744) else Color(0xFFFF9100)
                    val arrowSize = 14f

                    val p1 = edgePos + Offset(cos(angle) * arrowSize, sin(angle) * arrowSize)
                    val p2 = edgePos + Offset(cos(angle + 2.5f) * arrowSize, sin(angle + 2.5f) * arrowSize)
                    val p3 = edgePos + Offset(cos(angle - 2.5f) * arrowSize, sin(angle - 2.5f) * arrowSize)

                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }
                    scope.drawPath(path, color = arrowColor)
                    scope.drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3.5f, center = edgePos)
                }
            }
        }
    }

    private fun drawFood(
        scope: DrawScope,
        food: Food,
        screenPos: Offset,
        zoom: Float
    ) {
        val r = (food.radius * zoom).coerceAtLeast(3.5f)

        when (food.type) {
            FoodType.POTION -> {
                val color = food.primaryColor
                scope.drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = r * 1.8f,
                    center = screenPos
                )
                scope.drawCircle(
                    color = color,
                    radius = r,
                    center = screenPos
                )
                scope.drawRect(
                    color = Color(0xFFD7CCC8),
                    topLeft = screenPos - Offset(r * 0.35f, r * 1.35f),
                    size = Size(r * 0.7f, r * 0.5f)
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r * 0.35f,
                    center = screenPos - Offset(r * 0.3f, r * 0.3f)
                )
            }
            FoodType.DONUT -> {
                scope.drawCircle(
                    color = Color(0xFFD7A15C),
                    radius = r * 1.25f,
                    center = screenPos
                )
                scope.drawCircle(
                    color = food.primaryColor,
                    radius = r * 1.05f,
                    center = screenPos
                )
                scope.drawCircle(
                    color = bgColor,
                    radius = r * 0.38f,
                    center = screenPos
                )
                val spR = (r * 0.15f).coerceAtLeast(1.5f)
                scope.drawCircle(color = Color.White, radius = spR, center = screenPos + Offset(-r * 0.6f, -r * 0.4f))
                scope.drawCircle(color = Color(0xFFFFD700), radius = spR, center = screenPos + Offset(r * 0.45f, -r * 0.3f))
                scope.drawCircle(color = Color(0xFF00E5FF), radius = spR, center = screenPos + Offset(0f, r * 0.55f))
            }
            FoodType.WATERMELON -> {
                scope.drawArc(
                    color = Color(0xFF1B5E20),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = screenPos - Offset(r * 1.2f, r * 1.2f),
                    size = Size(r * 2.4f, r * 2.4f)
                )
                scope.drawArc(
                    color = Color(0xFFFF1744),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = screenPos - Offset(r * 0.95f, r * 0.95f),
                    size = Size(r * 1.9f, r * 1.9f)
                )
                val seedR = (r * 0.15f).coerceAtLeast(1.2f)
                scope.drawCircle(color = Color(0xFF212121), radius = seedR, center = screenPos + Offset(-r * 0.38f, r * 0.35f))
                scope.drawCircle(color = Color(0xFF212121), radius = seedR, center = screenPos + Offset(0f, r * 0.55f))
                scope.drawCircle(color = Color(0xFF212121), radius = seedR, center = screenPos + Offset(r * 0.38f, r * 0.35f))
            }
            FoodType.BURGER -> {
                scope.drawRoundRect(
                    color = Color(0xFFD7CCC8),
                    topLeft = screenPos - Offset(r, r * 0.3f),
                    size = Size(r * 2f, r * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
                scope.drawArc(
                    color = Color(0xFFFFB300),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = screenPos - Offset(r * 1.15f, r * 1.15f),
                    size = Size(r * 2.3f, r * 2.3f)
                )
                scope.drawRect(
                    color = Color(0xFF00E676),
                    topLeft = screenPos - Offset(r * 1.05f, 0f),
                    size = Size(r * 2.1f, r * 0.3f)
                )
                scope.drawRoundRect(
                    color = Color(0xFF5D4037),
                    topLeft = screenPos - Offset(r * 1.1f, r * 0.28f),
                    size = Size(r * 2.2f, r * 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * zoom, 3f * zoom)
                )
            }
            FoodType.CAKE -> {
                scope.drawRoundRect(
                    color = Color(0xFFE040FB),
                    topLeft = screenPos - Offset(r, r * 0.6f),
                    size = Size(r * 2f, r * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
                scope.drawRoundRect(
                    color = Color.White,
                    topLeft = screenPos - Offset(r * 1.05f, r * 0.9f),
                    size = Size(r * 2.1f, r * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
                scope.drawCircle(
                    color = Color(0xFFFF1744),
                    radius = r * 0.35f,
                    center = screenPos - Offset(0f, r * 1.15f)
                )
            }
            FoodType.STRAWBERRY -> {
                val strawberryPath = Path().apply {
                    moveTo(screenPos.x, screenPos.y + r * 1.3f)
                    cubicTo(
                        screenPos.x - r * 1.3f, screenPos.y + r * 0.6f,
                        screenPos.x - r * 1.2f, screenPos.y - r * 0.7f,
                        screenPos.x, screenPos.y - r * 0.7f
                    )
                    cubicTo(
                        screenPos.x + r * 1.2f, screenPos.y - r * 0.7f,
                        screenPos.x + r * 1.3f, screenPos.y + r * 0.6f,
                        screenPos.x, screenPos.y + r * 1.3f
                    )
                    close()
                }
                scope.drawPath(strawberryPath, color = Color(0xFFFF1744))
                scope.drawCircle(
                    color = Color(0xFF00E676),
                    radius = r * 0.4f,
                    center = screenPos - Offset(0f, r * 0.8f)
                )
                val dotR = (r * 0.12f).coerceAtLeast(1f)
                scope.drawCircle(color = Color(0xFFFFD54F), radius = dotR, center = screenPos + Offset(-r * 0.35f, 0f))
                scope.drawCircle(color = Color(0xFFFFD54F), radius = dotR, center = screenPos + Offset(r * 0.35f, 0f))
                scope.drawCircle(color = Color(0xFFFFD54F), radius = dotR, center = screenPos + Offset(0f, r * 0.5f))
            }
            FoodType.APPLE -> {
                scope.drawCircle(
                    color = Color(0xFFFF5252),
                    radius = r * 1.1f,
                    center = screenPos
                )
                scope.drawLine(
                    color = Color(0xFF5D4037),
                    start = screenPos - Offset(0f, r * 0.9f),
                    end = screenPos - Offset(0f, r * 1.4f),
                    strokeWidth = 2f * zoom
                )
                scope.drawCircle(
                    color = Color(0xFF76FF03),
                    radius = r * 0.35f,
                    center = screenPos - Offset(-r * 0.4f, r * 1.2f)
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = r * 0.3f,
                    center = screenPos - Offset(r * 0.35f, r * 0.35f)
                )
            }
            FoodType.MASS_DROP -> {
                scope.drawCircle(
                    color = food.primaryColor.copy(alpha = 0.45f),
                    radius = r * 1.6f,
                    center = screenPos
                )
                scope.drawCircle(
                    color = food.primaryColor,
                    radius = r,
                    center = screenPos
                )
                scope.drawCircle(
                    color = Color.White,
                    radius = r * 0.4f,
                    center = screenPos - Offset(r * 0.25f, r * 0.25f)
                )
            }
            else -> {
                scope.drawCircle(
                    color = food.primaryColor,
                    radius = r,
                    center = screenPos
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = r * 0.35f,
                    center = screenPos - Offset(r * 0.28f, r * 0.28f)
                )
            }
        }
    }

    private fun drawWorm(
        scope: DrawScope,
        worm: Worm,
        toScreen: (Vector2D) -> Offset,
        zoom: Float,
        viewportW: Float,
        viewportH: Float
    ) {
        val segCount = worm.segments.size
        if (segCount == 0) return

        val wormRadius = worm.currentRadius * zoom
        val skin = worm.skin
        val colors = skin.colors

        val headScreen = toScreen(worm.headPosition)

        // Worm Bounding Box Culling: if completely off-screen, skip entirely
        val margin = (wormRadius * segCount * 0.85f).coerceIn(350f, 1800f)
        if (!worm.isPlayer) {
            val isHeadOffscreen = headScreen.x < -margin || headScreen.x > viewportW + margin ||
                    headScreen.y < -margin || headScreen.y > viewportH + margin
            if (isHeadOffscreen) {
                // Quick check if any segment is on-screen
                var anyVisible = false
                val step = max(1, segCount / 6)
                for (s in 0 until segCount step step) {
                    val sp = toScreen(worm.segments[s])
                    if (sp.x >= -120f && sp.x <= viewportW + 120f && sp.y >= -120f && sp.y <= viewportH + 120f) {
                        anyVisible = true
                        break
                    }
                }
                if (!anyVisible) return
            }
        }

        // Spawn Shield
        if (worm.hasShield) {
            val shieldRadius = wormRadius * 1.9f
            scope.drawCircle(
                color = Color(0x5500E5FF),
                radius = shieldRadius,
                center = headScreen
            )
            scope.drawCircle(
                color = Color(0xFF00E5FF),
                radius = shieldRadius,
                center = headScreen,
                style = Stroke(width = 3.5f * zoom)
            )
        }

        // Boosting trail effect
        if (worm.isBoosting) {
            for (i in (segCount - 1) downTo 0 step 3) {
                val segPos = toScreen(worm.segments[i])
                if (segPos.x < -wormRadius * 2 || segPos.x > viewportW + wormRadius * 2 ||
                    segPos.y < -wormRadius * 2 || segPos.y > viewportH + wormRadius * 2
                ) continue
                scope.drawCircle(
                    color = Color(0x55FF9100),
                    radius = wormRadius * 1.4f,
                    center = segPos
                )
            }
        }

        // Draw segments from tail to head with segment-level culling
        for (i in (segCount - 1) downTo 0) {
            val segPos = toScreen(worm.segments[i])
            val radiusScale = if (i > segCount - 6) 0.65f + (segCount - 1 - i) * 0.07f else 1.0f
            val segRadius = wormRadius * radiusScale

            // Segment Culling: Skip drawing body segments that are outside screen viewport!
            if (segPos.x < -segRadius * 2 || segPos.x > viewportW + segRadius * 2 ||
                segPos.y < -segRadius * 2 || segPos.y > viewportH + segRadius * 2
            ) {
                continue
            }

            val segProgress = i.toFloat() / segCount.toFloat()
            val segColor = when (skin.pattern) {
                PatternType.STRIPES -> colors[(i / 3) % colors.size]
                PatternType.RAINBOW -> colors[(i / 2) % colors.size]
                PatternType.NEON_GLOW -> colors[i % colors.size]
                PatternType.CANDY_SWIRL -> if (i % 2 == 0) colors[0] else colors[min(1, colors.size - 1)]
                PatternType.DRAGON_SCALES -> colors[(i / 4) % colors.size]
                PatternType.POLKA_DOTS -> if (i % 3 == 0) colors.last() else colors[0]
                PatternType.GRADIENT -> {
                    val c1 = colors[0]
                    val c2 = colors.last()
                    Color(
                        red = c1.red + (c2.red - c1.red) * segProgress,
                        green = c1.green + (c2.green - c1.green) * segProgress,
                        blue = c1.blue + (c2.blue - c1.blue) * segProgress,
                        alpha = 1f
                    )
                }
                else -> colors[0]
            }

            scope.drawCircle(
                color = Color(0xFF141414),
                radius = segRadius + 2.5f * zoom,
                center = segPos
            )
            scope.drawCircle(
                color = segColor,
                radius = segRadius,
                center = segPos
            )
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.38f),
                radius = segRadius * 0.5f,
                center = segPos - Offset(segRadius * 0.28f, segRadius * 0.28f)
            )
        }

        // Draw Head (if on or near screen)
        val headRadius = wormRadius * 1.25f
        if (headScreen.x >= -headRadius * 2 && headScreen.x <= viewportW + headRadius * 2 &&
            headScreen.y >= -headRadius * 2 && headScreen.y <= viewportH + headRadius * 2
        ) {
            scope.drawCircle(
                color = Color(0xFF141414),
                radius = headRadius + 3f * zoom,
                center = headScreen
            )
            scope.drawCircle(
                color = colors.first(),
                radius = headRadius,
                center = headScreen
            )
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.42f),
                radius = headRadius * 0.52f,
                center = headScreen - Offset(headRadius * 0.28f, headRadius * 0.28f)
            )

            // Eyes
            val eyeAngle = worm.currentAngle
            val eyeDistance = headRadius * 0.52f
            val eyeSpreadAngle = 0.58f

            val leftEyePos = headScreen + Offset(cos(eyeAngle - eyeSpreadAngle) * eyeDistance, sin(eyeAngle - eyeSpreadAngle) * eyeDistance)
            val rightEyePos = headScreen + Offset(cos(eyeAngle + eyeSpreadAngle) * eyeDistance, sin(eyeAngle + eyeSpreadAngle) * eyeDistance)

            val eyeRadius = headRadius * 0.42f
            val pupilRadius = eyeRadius * 0.52f

            drawCartoonEye(scope, leftEyePos, eyeRadius, pupilRadius, eyeAngle, worm.blinkProgress, zoom)
            drawCartoonEye(scope, rightEyePos, eyeRadius, pupilRadius, eyeAngle, worm.blinkProgress, zoom)

            // Cheeks
            val blushDist = headRadius * 0.72f
            scope.drawCircle(color = Color(0x77FF4081), radius = headRadius * 0.24f, center = headScreen + Offset(cos(eyeAngle - 1.2f) * blushDist, sin(eyeAngle - 1.2f) * blushDist))
            scope.drawCircle(color = Color(0x77FF4081), radius = headRadius * 0.24f, center = headScreen + Offset(cos(eyeAngle + 1.2f) * blushDist, sin(eyeAngle + 1.2f) * blushDist))

            // Mouth
            drawWormMouth(scope, worm.face.type, headScreen, headRadius, eyeAngle, worm.isBoosting, worm.tongueTimer, zoom)

            // Hat
            drawHat(scope, worm.hat.type, headScreen, headRadius, eyeAngle, zoom)
        }
    }

    private fun drawCartoonEye(
        scope: DrawScope,
        pos: Offset,
        eyeRadius: Float,
        pupilRadius: Float,
        lookAngle: Float,
        blink: Float,
        zoom: Float
    ) {
        if (blink >= 0.75f) {
            val perp = lookAngle + (PI / 2f).toFloat()
            val dx = cos(perp) * eyeRadius
            val dy = sin(perp) * eyeRadius
            scope.drawLine(
                color = Color(0xFF1E1E24),
                start = pos - Offset(dx, dy),
                end = pos + Offset(dx, dy),
                strokeWidth = 3.5f * zoom
            )
            return
        }

        scope.drawCircle(color = Color(0xFF1E1E24), radius = eyeRadius + 1.5f * zoom, center = pos)
        scope.drawCircle(color = Color.White, radius = eyeRadius, center = pos)

        val pupilOffsetDist = eyeRadius * 0.35f
        val pupilCenter = pos + Offset(cos(lookAngle) * pupilOffsetDist, sin(lookAngle) * pupilOffsetDist)

        scope.drawCircle(color = Color(0xFF121214), radius = pupilRadius, center = pupilCenter)
        scope.drawCircle(color = Color.White, radius = pupilRadius * 0.45f, center = pupilCenter - Offset(pupilRadius * 0.32f, pupilRadius * 0.32f))
    }

    private fun drawWormMouth(
        scope: DrawScope,
        faceType: FaceType,
        headPos: Offset,
        headRadius: Float,
        angle: Float,
        isBoosting: Boolean,
        tongueTimer: Float,
        zoom: Float
    ) {
        val mouthDist = headRadius * 0.58f
        val mouthPos = headPos + Offset(cos(angle) * mouthDist, sin(angle) * mouthDist)

        if (isBoosting || faceType == FaceType.OPEN_MOUTH) {
            val mouthRadius = headRadius * 0.36f
            scope.drawCircle(color = Color(0xFF880E4F), radius = mouthRadius, center = mouthPos)
            scope.drawCircle(color = Color(0xFF212121), radius = mouthRadius, center = mouthPos, style = Stroke(width = 2f * zoom))
            scope.drawCircle(color = Color(0xFFFF4081), radius = mouthRadius * 0.6f, center = mouthPos + Offset(0f, mouthRadius * 0.3f))
            scope.drawRoundRect(
                color = Color.White,
                topLeft = mouthPos - Offset(mouthRadius * 0.35f, mouthRadius * 0.85f),
                size = Size(mouthRadius * 0.7f, mouthRadius * 0.45f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * zoom, 2f * zoom)
            )
        } else if (faceType == FaceType.SILLY_TONGUE) {
            val mouthW = headRadius * 0.55f
            scope.drawArc(
                color = Color(0xFF212121),
                startAngle = (angle * 180f / PI.toFloat()) - 45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = mouthPos - Offset(mouthW / 2f, mouthW / 2f),
                size = Size(mouthW, mouthW),
                style = Stroke(width = 3.5f * zoom)
            )
            val tongueDist = headRadius * 0.32f
            val tonguePos = mouthPos + Offset(cos(angle) * tongueDist, sin(angle) * tongueDist + 3f * zoom)
            scope.drawCircle(color = Color(0xFFFF4081), radius = headRadius * 0.22f, center = tonguePos)
        } else {
            val mouthW = headRadius * 0.6f
            scope.drawArc(
                color = Color(0xFF212121),
                startAngle = (angle * 180f / PI.toFloat()) - 50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = mouthPos - Offset(mouthW / 2f, mouthW / 2f),
                size = Size(mouthW, mouthW),
                style = Stroke(width = 3.5f * zoom)
            )
        }
    }

    private fun drawHat(
        scope: DrawScope,
        hatType: HatType,
        headPos: Offset,
        headRadius: Float,
        angle: Float,
        zoom: Float
    ) {
        val hatOffsetDist = headRadius * 0.78f
        val hatCenter = headPos - Offset(cos(angle) * (hatOffsetDist * 0.3f), sin(angle) * (hatOffsetDist * 0.3f) + hatOffsetDist)

        when (hatType) {
            HatType.CROWN -> {
                val crownW = headRadius * 1.15f
                val crownH = headRadius * 0.65f
                val crownPath = Path().apply {
                    moveTo(hatCenter.x - crownW / 2f, hatCenter.y)
                    lineTo(hatCenter.x - crownW / 2f, hatCenter.y - crownH)
                    lineTo(hatCenter.x - crownW / 4f, hatCenter.y - crownH * 0.45f)
                    lineTo(hatCenter.x, hatCenter.y - crownH * 1.1f)
                    lineTo(hatCenter.x + crownW / 4f, hatCenter.y - crownH * 0.45f)
                    lineTo(hatCenter.x + crownW / 2f, hatCenter.y - crownH)
                    lineTo(hatCenter.x + crownW / 2f, hatCenter.y)
                    close()
                }
                scope.drawPath(crownPath, color = Color(0xFFFFD700))
                scope.drawPath(crownPath, color = Color(0xFF5D4037), style = Stroke(width = 2f * zoom))
                scope.drawCircle(color = Color(0xFFFF1744), radius = 3.5f * zoom, center = hatCenter + Offset(0f, -crownH * 1.1f))
                scope.drawCircle(color = Color(0xFF00E5FF), radius = 3f * zoom, center = hatCenter + Offset(-crownW / 2f, -crownH))
                scope.drawCircle(color = Color(0xFF00E5FF), radius = 3f * zoom, center = hatCenter + Offset(crownW / 2f, -crownH))
            }
            HatType.SUNGLASSES -> {
                val glassW = headRadius * 0.95f
                val glassH = headRadius * 0.4f
                val glassCenter = headPos + Offset(cos(angle) * headRadius * 0.4f, sin(angle) * headRadius * 0.4f - headRadius * 0.15f)
                scope.drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = glassCenter - Offset(glassW / 2f, glassH / 2f),
                    size = Size(glassW, glassH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
                scope.drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = glassCenter - Offset(glassW * 0.42f, glassH * 0.35f),
                    size = Size(glassW * 0.35f, glassH * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * zoom, 3f * zoom)
                )
                scope.drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = glassCenter + Offset(glassW * 0.08f, -glassH * 0.35f),
                    size = Size(glassW * 0.35f, glassH * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * zoom, 3f * zoom)
                )
            }
            HatType.PARTY_HAT -> {
                val w = headRadius * 0.8f
                val h = headRadius * 1.1f
                val conePath = Path().apply {
                    moveTo(hatCenter.x - w / 2f, hatCenter.y + h / 2f)
                    lineTo(hatCenter.x, hatCenter.y - h / 2f)
                    lineTo(hatCenter.x + w / 2f, hatCenter.y + h / 2f)
                    close()
                }
                scope.drawPath(conePath, color = Color(0xFFFF007F))
                scope.drawPath(conePath, color = Color(0xFF212121), style = Stroke(width = 2f * zoom))
                scope.drawCircle(color = Color(0xFFFFEA00), radius = 6f * zoom, center = hatCenter - Offset(0f, h / 2f))
            }
            HatType.BASEBALL_CAP -> {
                val w = headRadius * 0.9f
                val h = headRadius * 0.55f
                scope.drawArc(
                    color = Color(0xFFFF1744),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = hatCenter - Offset(w / 2f, h * 0.6f),
                    size = Size(w, h)
                )
                scope.drawRoundRect(
                    color = Color(0xFFB71C1C),
                    topLeft = hatCenter - Offset(w * 0.6f, -h * 0.2f),
                    size = Size(w * 0.8f, h * 0.25f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
            }
            HatType.CAT_EARS -> {
                val earH = headRadius * 0.5f
                val leftPath = Path().apply {
                    moveTo(hatCenter.x - headRadius * 0.6f, hatCenter.y + earH / 2f)
                    lineTo(hatCenter.x - headRadius * 0.35f, hatCenter.y - earH)
                    lineTo(hatCenter.x - headRadius * 0.1f, hatCenter.y + earH / 2f)
                    close()
                }
                scope.drawPath(leftPath, color = Color(0xFF212121))
                val rightPath = Path().apply {
                    moveTo(hatCenter.x + headRadius * 0.1f, hatCenter.y + earH / 2f)
                    lineTo(hatCenter.x + headRadius * 0.35f, hatCenter.y - earH)
                    lineTo(hatCenter.x + headRadius * 0.6f, hatCenter.y + earH / 2f)
                    close()
                }
                scope.drawPath(rightPath, color = Color(0xFF212121))
                scope.drawCircle(color = Color(0xFFFF80AB), radius = 4f * zoom, center = hatCenter - Offset(headRadius * 0.35f, earH * 0.3f))
                scope.drawCircle(color = Color(0xFFFF80AB), radius = 4f * zoom, center = hatCenter + Offset(headRadius * 0.35f, -earH * 0.3f))
            }
            HatType.ANGEL_HALO -> {
                scope.drawOval(
                    color = Color(0xFFFFD700),
                    topLeft = hatCenter - Offset(headRadius * 0.55f, headRadius * 0.25f),
                    size = Size(headRadius * 1.1f, headRadius * 0.45f),
                    style = Stroke(width = 4f * zoom)
                )
            }
            HatType.VIKING_HORNS -> {
                scope.drawArc(
                    color = Color(0xFFEEEEEE),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = hatCenter - Offset(headRadius * 0.68f, headRadius * 0.5f),
                    size = Size(headRadius * 0.45f, headRadius * 0.55f),
                    style = Stroke(width = 6f * zoom)
                )
                scope.drawArc(
                    color = Color(0xFFEEEEEE),
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = hatCenter + Offset(headRadius * 0.25f, -headRadius * 0.5f),
                    size = Size(headRadius * 0.45f, headRadius * 0.55f),
                    style = Stroke(width = 6f * zoom)
                )
            }
            HatType.PIRATE_HAT -> {
                val w = headRadius * 1.2f
                val h = headRadius * 0.6f
                val hatPath = Path().apply {
                    moveTo(hatCenter.x - w / 2f, hatCenter.y + h / 3f)
                    quadraticBezierTo(hatCenter.x, hatCenter.y - h / 2f, hatCenter.x + w / 2f, hatCenter.y + h / 3f)
                    lineTo(hatCenter.x, hatCenter.y + h / 2f)
                    close()
                }
                scope.drawPath(hatPath, color = Color(0xFF1B1B1B))
                scope.drawCircle(color = Color.White, radius = 4f * zoom, center = hatCenter)
            }
            HatType.NINJA_HEADBAND -> {
                val bandW = headRadius * 1.1f
                val bandH = headRadius * 0.32f
                scope.drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = hatCenter - Offset(bandW / 2f, bandH / 2f),
                    size = Size(bandW, bandH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * zoom, 3f * zoom)
                )
                // Center metallic badge
                scope.drawCircle(color = Color(0xFFEEEEEE), radius = bandH * 0.4f, center = hatCenter)
                // Trailing ribbons
                scope.drawLine(
                    color = Color(0xFFFF1744),
                    start = hatCenter + Offset(bandW / 2f - 2f, 0f),
                    end = hatCenter + Offset(bandW / 2f + headRadius * 0.5f, headRadius * 0.4f),
                    strokeWidth = 3f * zoom
                )
            }
            HatType.WIZARD_HAT -> {
                val w = headRadius * 1.0f
                val h = headRadius * 1.2f
                val hatPath = Path().apply {
                    moveTo(hatCenter.x - w / 2f, hatCenter.y + h * 0.3f)
                    lineTo(hatCenter.x + headRadius * 0.15f, hatCenter.y - h * 0.7f)
                    lineTo(hatCenter.x + w / 2f, hatCenter.y + h * 0.3f)
                    close()
                }
                scope.drawPath(hatPath, color = Color(0xFF311B92))
                scope.drawOval(
                    color = Color(0xFF1A237E),
                    topLeft = hatCenter - Offset(w * 0.65f, -h * 0.2f),
                    size = Size(w * 1.3f, h * 0.25f)
                )
                scope.drawCircle(color = Color(0xFFFFD700), radius = 4f * zoom, center = hatCenter - Offset(0f, h * 0.1f))
            }
            HatType.DEVIL_HORNS -> {
                val hornW = headRadius * 0.35f
                val hornH = headRadius * 0.6f
                scope.drawArc(
                    color = Color(0xFFFF1744),
                    startAngle = 170f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = hatCenter - Offset(headRadius * 0.65f, hornH * 0.8f),
                    size = Size(hornW, hornH),
                    style = Stroke(width = 6f * zoom)
                )
                scope.drawArc(
                    color = Color(0xFFFF1744),
                    startAngle = 270f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = hatCenter + Offset(headRadius * 0.3f, -hornH * 0.8f),
                    size = Size(hornW, hornH),
                    style = Stroke(width = 6f * zoom)
                )
            }
            HatType.COWBOY_HAT -> {
                val brimW = headRadius * 1.35f
                val crownW = headRadius * 0.75f
                scope.drawOval(
                    color = Color(0xFF5D4037),
                    topLeft = hatCenter - Offset(brimW / 2f, headRadius * 0.15f),
                    size = Size(brimW, headRadius * 0.35f)
                )
                scope.drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = hatCenter - Offset(crownW / 2f, headRadius * 0.6f),
                    size = Size(crownW, headRadius * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * zoom, 4f * zoom)
                )
            }
            HatType.CHEF_HAT -> {
                val toqueW = headRadius * 0.9f
                val toqueH = headRadius * 0.75f
                scope.drawRoundRect(
                    color = Color(0xFFF5F5F5),
                    topLeft = hatCenter - Offset(toqueW / 2f, toqueH),
                    size = Size(toqueW, toqueH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * zoom, 8f * zoom)
                )
                scope.drawCircle(color = Color.White, radius = toqueW * 0.35f, center = hatCenter - Offset(-toqueW * 0.25f, toqueH * 0.9f))
                scope.drawCircle(color = Color.White, radius = toqueW * 0.35f, center = hatCenter - Offset(toqueW * 0.25f, toqueH * 0.9f))
                scope.drawCircle(color = Color.White, radius = toqueW * 0.38f, center = hatCenter - Offset(0f, toqueH * 1.05f))
            }
            else -> {}
        }
    }

    private fun drawDeathGhosts(
        scope: DrawScope,
        world: GameWorld,
        toScreen: (Vector2D) -> Offset,
        zoom: Float,
        viewportW: Float,
        viewportH: Float
    ) {
        for (ghost in world.deathGhosts) {
            val alpha = (ghost.life / ghost.maxLife).coerceIn(0f, 1f)
            val segRadius = ghost.headRadius * zoom * (0.85f + (1f - alpha) * 0.35f)
            val fadeColor = ghost.color.copy(alpha = alpha * 0.65f)
            val step = max(1, ghost.segments.size / 22)
            for (i in 0 until ghost.segments.size step step) {
                val sp = toScreen(ghost.segments[i])
                if (sp.x < -segRadius || sp.x > viewportW + segRadius ||
                    sp.y < -segRadius || sp.y > viewportH + segRadius
                ) continue
                scope.drawCircle(color = fadeColor, radius = segRadius, center = sp)
                scope.drawCircle(color = Color.White.copy(alpha = alpha * 0.35f), radius = segRadius * 0.4f, center = sp)
            }
        }
    }

    private fun drawDeathParticles(
        scope: DrawScope,
        world: GameWorld,
        toScreen: (Vector2D) -> Offset,
        zoom: Float,
        viewportW: Float,
        viewportH: Float
    ) {
        for (p in world.deathParticles) {
            val sp = toScreen(p.pos)
            val r = (p.radius * zoom).coerceAtLeast(2.5f)
            if (sp.x < -r || sp.x > viewportW + r || sp.y < -r || sp.y > viewportH + r) continue
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            scope.drawCircle(color = p.color.copy(alpha = alpha * 0.85f), radius = r, center = sp)
            scope.drawCircle(color = Color.White.copy(alpha = alpha * 0.6f), radius = r * 0.4f, center = sp)
        }
    }
}
