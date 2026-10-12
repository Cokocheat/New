package com.example.game.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.*

class Worm(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val isPlayer: Boolean = false,
    var skin: Skin,
    var hat: Hat = Catalog.HATS[0],
    var face: Face = Catalog.FACES[0],
    var headPosition: Vector2D = Vector2D(0f, 0f),
    initialSegmentsCount: Int = 26
) {
    // Segments trail
    val segments = ArrayList<Vector2D>()
    var segmentSpacing: Float = 13f
    var baseRadius: Float = 22f

    // Movement & Orientation
    var currentAngle: Float = 0f // in radians
    var targetAngle: Float = 0f
    var turnSpeed: Float = 7.5f // Responsive and silky-smooth steering

    // Speed
    var normalSpeed: Float = 180f // world units/sec
    var boostSpeedMultiplier: Float = 1.9f
    var isBoosting: Boolean = false

    // Stats
    var score: Int = 100
    var kills: Int = 0
    var isAlive: Boolean = true

    // Invulnerability shield on spawn (3.5 seconds)
    var spawnShieldTimer: Float = 3.5f
    val hasShield: Boolean
        get() = spawnShieldTimer > 0f || hasPotion(PotionType.SHIELD)

    // Expressions & Animation
    var blinkProgress: Float = 0f // 0 = open, 1 = closed
    var nextBlinkTimer: Float = 2.2f
    var tongueTimer: Float = 0f
    var slitherWiggleTimer: Float = 0f

    // Active Potions
    val activePotions = HashMap<PotionType, ActivePotion>()

    // AI Bot State (if !isPlayer)
    var botTarget: Vector2D? = null
    var botDecisionTimer: Float = 0f
    var botAggressiveness: Float = 0.5f // 0 = cautious food seeker, 1 = predator hunter
    var botWanderAngle: Float = 0f

    init {
        // Initialize segments behind head
        segments.clear()
        for (i in 0 until initialSegmentsCount) {
            val offset = Vector2D(
                headPosition.x - cos(currentAngle) * (i * segmentSpacing),
                headPosition.y - sin(currentAngle) * (i * segmentSpacing)
            )
            segments.add(offset)
        }
    }

    val currentRadius: Float
        get() = (baseRadius + min(20f, log10(max(10f, score.toFloat())) * 3.8f)).coerceIn(18f, 42f)

    fun hasPotion(type: PotionType): Boolean = activePotions.containsKey(type)

    fun addPotion(type: PotionType) {
        activePotions[type] = ActivePotion(type, type.durationSec)
    }

    fun updatePotions(deltaTime: Float) {
        if (spawnShieldTimer > 0f) {
            spawnShieldTimer = (spawnShieldTimer - deltaTime).coerceAtLeast(0f)
        }

        val iterator = activePotions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.remainingSeconds -= deltaTime
            if (entry.value.remainingSeconds <= 0f) {
                iterator.remove()
            }
        }
    }

    fun steerTowards(target: Vector2D) {
        val diff = target - headPosition
        targetAngle = atan2(diff.y, diff.x)
    }

    fun steerAngle(angleRad: Float) {
        targetAngle = angleRad
    }

    fun updateMovement(deltaTime: Float) {
        if (!isAlive) return

        // Smooth angle rotation (shortest path)
        var diff = targetAngle - currentAngle
        while (diff > PI) diff -= (2 * PI).toFloat()
        while (diff < -PI) diff += (2 * PI).toFloat()

        val actualTurnSpeed = if (isBoosting) turnSpeed * 1.15f else turnSpeed
        val maxTurn = actualTurnSpeed * deltaTime
        currentAngle += diff.coerceIn(-maxTurn, maxTurn)

        // Effective speed
        var speed = normalSpeed
        if (hasPotion(PotionType.SPEED_BOOST)) {
            speed *= 1.4f
        }
        if (isBoosting) {
            speed *= boostSpeedMultiplier
        }

        // Head step forward
        val moveVector = Vector2D.fromAngle(currentAngle, speed * deltaTime)
        val newHead = headPosition + moveVector
        headPosition = newHead

        // Update trail segments using inverse distance constraint
        if (segments.isEmpty()) {
            segments.add(headPosition)
        } else {
            // First segment follows head smoothly
            var prev = headPosition
            for (i in 0 until segments.size) {
                val current = segments[i]
                val toPrev = prev - current
                val dist = toPrev.length()
                if (dist > segmentSpacing) {
                    val dir = toPrev.normalized()
                    segments[i] = prev - (dir * segmentSpacing)
                }
                prev = segments[i]
            }
        }

        // Slither wiggle & facial animations
        slitherWiggleTimer += deltaTime * (if (isBoosting) 18f else 10f)
        tongueTimer += deltaTime
        nextBlinkTimer -= deltaTime
        if (nextBlinkTimer <= 0f) {
            blinkProgress = 1f
            nextBlinkTimer = (2.0f + Math.random().toFloat() * 3.5f)
        } else if (blinkProgress > 0f) {
            blinkProgress = (blinkProgress - deltaTime * 6f).coerceAtLeast(0f)
        }
    }

    fun grow(points: Int) {
        score += points
        // Calculate desired segments based on score
        val targetSegments = (26 + (score / 30)).coerceAtMost(400)
        while (segments.size < targetSegments) {
            val tail = segments.lastOrNull() ?: headPosition
            segments.add(tail)
        }
    }

    fun shrinkForBoost(cost: Int) {
        if (score > 120) {
            score -= cost
            val targetSegments = (26 + (score / 30)).coerceAtMost(400)
            if (segments.size > targetSegments && segments.size > 26) {
                segments.removeAt(segments.lastIndex)
            }
        }
    }
}
