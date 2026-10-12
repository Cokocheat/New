package com.example.game.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.game.model.*
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class GameWorld(
    val soundManager: SoundManager,
    var playerName: String = "Nana",
    var selectedSkin: Skin = Catalog.SKINS[0],
    var selectedHat: Hat = Catalog.HATS[0],
    var selectedFace: Face = Catalog.FACES[0]
) {
    // World bounds: Vast, expansive arena (Radius 5600f, diameter 11200f)
    val arenaRadius: Float = 5600f
    val arenaSize: Float = 11200f

    // Entities
    var playerWorm: Worm = createPlayer()
    val botWorms = ArrayList<Worm>()
    val foods = ArrayList<Food>()

    // Spatial hash grid for ultra-smooth 60fps food lookups
    private val cellSize = 320f
    private val spatialGrid = HashMap<Long, ArrayList<Food>>()

    // Observable states for Jetpack Compose UI
    var isGameOver by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var coinsCollected by mutableStateOf(0)
    var sessionKills by mutableStateOf(0)
    var sessionMaxScore by mutableStateOf(100)
    var sessionDurationSec: Float = 0f

    // In-game kill notification banner
    var activeKillNotice by mutableStateOf<KillNotice?>(null)
    private var killNoticeTimer: Float = 0f

    // Smooth death animations (ghost trail + exploding particles)
    val deathParticles = ArrayList<DeathParticle>()
    val deathGhosts = ArrayList<DeathGhostEffect>()

    // Revive with Coins state
    var isRevivePromptActive by mutableStateOf(false)
    var reviveCountdownSec by mutableStateOf(5.0f)

    // Radar scanning angle
    var radarScanAngle: Float = 0f

    // Food limits (Rich, abundant feast across the vast map)
    private val maxFoodCount = 1600
    private var boostDropCounter = 0

    // Gamer names (Indonesian & Global mix for multiplayer room feel)
    private val botNames = listOf(
        "RajaCacing", "BudiSantoso", "SitiGamer", "ProSlither99", "DragonSlayer",
        "WormMasterID", "NasiGoreng", "ViperKing", "SpeedyWorm", "GhostRider",
        "MegaSnake", "SuperNana", "CacingKuning", "GarudaGamer", "CyberNinja",
        "TokyoDrift", "AuraFire", "EvosLegend", "RexRegum", "BigBoss99",
        "WormyCute", "ZodiacID", "BlitzStorm", "ApexHunter", "CandyLover",
        "ShadowHunter", "NovaStar", "TitanWorm", "VortexID", "CrimsonViper",
        "PhantomPro", "SkyWalker", "KrakenID", "HydraSnake", "AlphaWolf"
    )

    private val countryCodes = listOf(
        "ID", "ID", "ID", "MY", "SG", "TH", "PH", "JP", "KR", "US", "GB", "BR"
    )

    init {
        resetGame()
    }

    private fun getGridKey(x: Float, y: Float): Long {
        val gx = floor(x / cellSize).toInt()
        val gy = floor(y / cellSize).toInt()
        return (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
    }

    private fun rebuildSpatialGrid() {
        spatialGrid.clear()
        for (food in foods) {
            val key = getGridKey(food.position.x, food.position.y)
            var list = spatialGrid[key]
            if (list == null) {
                list = ArrayList(16)
                spatialGrid[key] = list
            }
            list.add(food)
        }
    }

    fun resetGame() {
        isGameOver = false
        isPaused = false
        isRevivePromptActive = false
        reviveCountdownSec = 5.0f
        activeKillNotice = null
        killNoticeTimer = 0f
        deathParticles.clear()
        deathGhosts.clear()
        sessionDurationSec = 0f
        sessionKills = 0
        coinsCollected = 0
        sessionMaxScore = 100
        radarScanAngle = 0f

        // Create player with safe spawn shield at center
        playerWorm = createPlayer()

        // Create 34 active bot worms distributed across the expansive map
        botWorms.clear()
        val botCount = 34
        for (i in 0 until botCount) {
            spawnBot()
        }

        // Spawn initial food across the vast map
        foods.clear()
        for (i in 0 until maxFoodCount) {
            foods.add(createRandomFood())
        }
        rebuildSpatialGrid()
    }

    private fun createPlayer(): Worm {
        val worm = Worm(
            name = playerName.ifBlank { "Nana" },
            isPlayer = true,
            skin = selectedSkin,
            hat = selectedHat,
            face = selectedFace,
            headPosition = Vector2D(0f, 0f),
            initialSegmentsCount = 28
        )
        worm.score = 100
        worm.spawnShieldTimer = 4.5f // 4.5s safe spawn shield
        return worm
    }

    private fun spawnBot(): Worm {
        // Guarantee bots spawn away from the center where player starts
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val dist = 650f + Random.nextFloat() * (arenaRadius * 0.90f - 650f)
        val pos = Vector2D(cos(angle) * dist, sin(angle) * dist)

        val name = botNames.random()
        val skin = Catalog.SKINS.random()
        val hat = if (Random.nextFloat() > 0.35f) Catalog.HATS.random() else Catalog.HATS[0]
        val face = Catalog.FACES.random()
        val initialSegments = Random.nextInt(22, 75)

        val bot = Worm(
            name = name,
            isPlayer = false,
            skin = skin,
            hat = hat,
            face = face,
            headPosition = pos,
            initialSegmentsCount = initialSegments
        )
        bot.score = initialSegments * 20
        bot.currentAngle = Random.nextFloat() * 2 * PI.toFloat()
        bot.targetAngle = bot.currentAngle
        bot.botAggressiveness = Random.nextFloat()
        bot.spawnShieldTimer = 2.0f
        botWorms.add(bot)
        return bot
    }

    private fun createRandomFood(center: Vector2D? = null, spread: Float = 0f): Food {
        val pos = if (center != null) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val r = Random.nextFloat() * spread
            Vector2D(center.x + cos(angle) * r, center.y + sin(angle) * r)
        } else {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val r = sqrt(Random.nextFloat()) * (arenaRadius - 80f)
            Vector2D(cos(angle) * r, sin(angle) * r)
        }

        // 6% chance for magical potion
        if (Random.nextFloat() < 0.06f) {
            val potionType = PotionType.values().random()
            return Food(
                position = pos,
                type = FoodType.POTION,
                potionType = potionType,
                value = 50,
                radius = 16f,
                primaryColor = Color(potionType.colorHex)
            )
        }

        // Tasty snack types
        val types = listOf(FoodType.APPLE, FoodType.WATERMELON, FoodType.STRAWBERRY, FoodType.DONUT, FoodType.BURGER, FoodType.CAKE)
        val selectedType = types.random()
        val (color, radius, valPoints) = when (selectedType) {
            FoodType.DONUT -> Triple(Color(0xFFFF4081), 15f, 25)
            FoodType.WATERMELON -> Triple(Color(0xFF00E676), 14f, 20)
            FoodType.BURGER -> Triple(Color(0xFFFFB300), 16f, 24)
            FoodType.CAKE -> Triple(Color(0xFFE040FB), 16f, 30)
            FoodType.STRAWBERRY -> Triple(Color(0xFFFF1744), 13f, 15)
            else -> Triple(Color(0xFFFF5252), 13f, 12)
        }

        return Food(
            position = pos,
            type = selectedType,
            value = valPoints,
            radius = radius,
            primaryColor = color
        )
    }

    fun update(deltaTime: Float) {
        if (isPaused) return

        // Handle revive countdown
        if (isRevivePromptActive) {
            reviveCountdownSec -= deltaTime
            if (reviveCountdownSec <= 0f) {
                declineRevive()
            }
            return
        }

        if (isGameOver) return

        sessionDurationSec += deltaTime
        radarScanAngle = (radarScanAngle + deltaTime * 2.8f) % (2 * PI.toFloat())

        // Update active kill notification timer
        if (activeKillNotice != null) {
            killNoticeTimer -= deltaTime
            if (killNoticeTimer <= 0f) {
                activeKillNotice = null
            }
        }

        // Update smooth death particles
        val pIter = deathParticles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.pos = p.pos + p.velocity * deltaTime
            p.velocity = p.velocity * 0.94f
            p.life -= deltaTime
            if (p.life <= 0f) pIter.remove()
        }

        // Update death ghost fading effects
        val gIter = deathGhosts.iterator()
        while (gIter.hasNext()) {
            val g = gIter.next()
            g.life -= deltaTime
            if (g.life <= 0f) gIter.remove()
        }

        // 1. Update Player
        if (playerWorm.isAlive) {
            playerWorm.updateMovement(deltaTime)
            playerWorm.updatePotions(deltaTime)

            // Arena boundary bounce for player
            val distFromCenter = playerWorm.headPosition.length()
            if (distFromCenter > arenaRadius) {
                playerWorm.headPosition = playerWorm.headPosition.normalized() * arenaRadius
                playerWorm.steerTowards(Vector2D(0f, 0f))
            }

            // Boosting drops mass crumbs
            if (playerWorm.isBoosting) {
                boostDropCounter++
                if (boostDropCounter % 4 == 0) {
                    playerWorm.shrinkForBoost(2)
                    val dropPos = playerWorm.segments.lastOrNull() ?: playerWorm.headPosition
                    val drop = Food(
                        position = dropPos + Vector2D(Random.nextFloat() * 10f - 5f, Random.nextFloat() * 10f - 5f),
                        type = FoodType.MASS_DROP,
                        value = 8,
                        radius = 8f,
                        primaryColor = playerWorm.skin.colors.first()
                    )
                    foods.add(drop)
                }
            }

            if (playerWorm.score > sessionMaxScore) {
                sessionMaxScore = playerWorm.score
            }
        }

        // 2. Update Bots AI
        updateBots(deltaTime)

        // 3. Magnet effect for player
        if (playerWorm.isAlive && playerWorm.hasPotion(PotionType.MAGNET)) {
            val magnetRadius = 360f
            val head = playerWorm.headPosition
            for (food in foods) {
                val d = food.position.distanceTo(head)
                if (d < magnetRadius && d > 5f) {
                    val pullDir = (head - food.position).normalized()
                    food.position = food.position + pullDir * (450f * deltaTime)
                }
            }
        }

        // 4. Food consumption (spatial lookup for high 60fps performance)
        checkFoodCollisions(playerWorm)
        for (bot in botWorms) {
            if (bot.isAlive) {
                checkFoodCollisions(bot)
            }
        }

        // 5. Worm vs Worm collisions (head-to-body)
        checkWormCollisions()

        // 6. Maintain food quantity in the vast map
        var addedAny = false
        while (foods.size < maxFoodCount) {
            foods.add(createRandomFood())
            addedAny = true
        }
        if (addedAny || (sessionDurationSec.toInt() % 2 == 0)) {
            rebuildSpatialGrid()
        }

        // 7. Respawn dead bots
        val deadBots = botWorms.filter { !it.isAlive }
        for (dead in deadBots) {
            botWorms.remove(dead)
            spawnBot()
        }
    }

    private fun updateBots(deltaTime: Float) {
        for (bot in botWorms) {
            if (!bot.isAlive) continue

            bot.updateMovement(deltaTime)
            bot.updatePotions(deltaTime)

            val dist = bot.headPosition.length()
            if (dist > arenaRadius - 100f) {
                bot.steerTowards(Vector2D(0f, 0f))
                continue
            }

            bot.botDecisionTimer -= deltaTime
            if (bot.botDecisionTimer <= 0f) {
                bot.botDecisionTimer = Random.nextFloat() * 0.7f + 0.35f

                var dangerVector: Vector2D? = null
                val allWorms = ArrayList<Worm>()
                if (playerWorm.isAlive) allWorms.add(playerWorm)
                allWorms.addAll(botWorms)

                val head = bot.headPosition
                val checkRadius = 95f

                for (other in allWorms) {
                    if (other.id == bot.id) continue
                    for (i in 0 until other.segments.size step 2) {
                        val seg = other.segments[i]
                        val segDist = head.distanceTo(seg)
                        if (segDist < checkRadius) {
                            dangerVector = head - seg
                            break
                        }
                    }
                    if (dangerVector != null) break
                }

                if (dangerVector != null) {
                    val avoidAngle = atan2(dangerVector.y, dangerVector.x)
                    bot.steerAngle(avoidAngle)
                    bot.isBoosting = bot.botAggressiveness > 0.6f
                } else {
                    var closestFood: Food? = null
                    var closestDist = 340f
                    val sampleStep = max(1, foods.size / 60)
                    for (i in 0 until foods.size step sampleStep) {
                        val f = foods[i]
                        val d = head.distanceTo(f.position)
                        if (d < closestDist) {
                            closestDist = d
                            closestFood = f
                        }
                    }

                    if (closestFood != null) {
                        bot.steerTowards(closestFood.position)
                    } else {
                        bot.botWanderAngle += (Random.nextFloat() - 0.5f) * 1.2f
                        bot.steerAngle(bot.botWanderAngle)
                    }
                    bot.isBoosting = false
                }
            }
        }
    }

    private fun checkFoodCollisions(worm: Worm) {
        val head = worm.headPosition
        val eatRadius = worm.currentRadius + 18f
        val eatRadiusSq = eatRadius * eatRadius

        // Query spatial grid around head (3x3 cells)
        val hx = head.x
        val hy = head.y
        val curGx = floor(hx / cellSize).toInt()
        val curGy = floor(hy / cellSize).toInt()

        var ateAny = false

        for (dx in -1..1) {
            for (dy in -1..1) {
                val key = ((curGx + dx).toLong() shl 32) or ((curGy + dy).toLong() and 0xFFFFFFFFL)
                val cellList = spatialGrid[key] ?: continue

                val iterator = cellList.iterator()
                while (iterator.hasNext()) {
                    val food = iterator.next()
                    if (head.distanceSquaredTo(food.position) <= eatRadiusSq) {
                        iterator.remove()
                        foods.remove(food)
                        ateAny = true

                        var pts = food.value
                        if (worm.hasPotion(PotionType.MULTIPLIER_2X)) {
                            pts *= 2
                        }
                        worm.grow(pts)

                        if (food.type == FoodType.POTION && food.potionType != null) {
                            worm.addPotion(food.potionType)
                            if (worm.isPlayer) {
                                soundManager.playPotion()
                            }
                        }

                        if (worm.isPlayer) {
                            coinsCollected += max(1, food.value / 10)
                        }
                    }
                }
            }
        }

        if (ateAny && worm.isPlayer) {
            soundManager.playEat()
        }
    }

    private fun checkWormCollisions() {
        val allWorms = ArrayList<Worm>()
        if (playerWorm.isAlive) allWorms.add(playerWorm)
        allWorms.addAll(botWorms.filter { it.isAlive })

        for (attacker in allWorms) {
            if (!attacker.isAlive) continue
            if (attacker.hasShield) continue

            val attackerHead = attacker.headPosition
            val attackerRadius = attacker.currentRadius * 0.72f

            for (victim in allWorms) {
                if (!victim.isAlive || attacker.id == victim.id) continue

                val segs = victim.segments
                val victimRadius = victim.currentRadius
                val hitDistanceThreshold = (attackerRadius + victimRadius * 0.7f)
                val hitDistSq = hitDistanceThreshold * hitDistanceThreshold

                for (i in 3 until segs.size step 2) {
                    val seg = segs[i]
                    if (attackerHead.distanceSquaredTo(seg) <= hitDistSq) {
                        killWorm(attacker, killer = victim)
                        break
                    }
                }

                if (!attacker.isAlive) break
            }
        }
    }

    private fun killWorm(deadWorm: Worm, killer: Worm?) {
        deadWorm.isAlive = false

        // Spawn smooth fading ghost segments
        deathGhosts.add(
            DeathGhostEffect(
                segments = ArrayList(deadWorm.segments),
                color = deadWorm.skin.colors.first(),
                headRadius = deadWorm.currentRadius
            )
        )

        // Spawn smooth bursting particles
        for (i in 0 until 28) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = 80f + Random.nextFloat() * 240f
            deathParticles.add(
                DeathParticle(
                    pos = deadWorm.headPosition,
                    velocity = Vector2D(cos(angle) * speed, sin(angle) * speed),
                    color = deadWorm.skin.colors.random(),
                    radius = Random.nextFloat() * 7f + 4f
                )
            )
        }

        if (killer != null) {
            killer.kills++
            killer.grow(deadWorm.score / 2)
            if (killer.isPlayer) {
                sessionKills++
                coinsCollected += 50
                activeKillNotice = KillNotice(victimName = deadWorm.name, bonusCoins = 50)
                killNoticeTimer = 2.8f
                soundManager.playKill()
            }
        }

        if (deadWorm.isPlayer || killer?.isPlayer == true) {
            soundManager.playCrash()
        }

        // Spawn food remains from dead worm's body segments
        val dropCount = min(40, max(12, deadWorm.segments.size / 2))
        val step = max(1, deadWorm.segments.size / dropCount)
        for (i in 0 until deadWorm.segments.size step step) {
            val seg = deadWorm.segments[i]
            val f = createRandomFood(center = seg, spread = 24f).copy(
                value = 35,
                radius = 17f,
                primaryColor = deadWorm.skin.colors.first()
            )
            foods.add(f)
        }

        if (deadWorm.isPlayer) {
            isRevivePromptActive = true
            reviveCountdownSec = 5.0f
        }
    }

    fun revivePlayer() {
        isRevivePromptActive = false
        isGameOver = false
        playerWorm.isAlive = true
        playerWorm.spawnShieldTimer = 5.0f // 5s invincible shield

        // Clear immediate danger: repel any bots nearby
        val pHead = playerWorm.headPosition
        for (bot in botWorms) {
            if (bot.headPosition.distanceTo(pHead) < 500f) {
                val away = (bot.headPosition - pHead).normalized()
                bot.headPosition = pHead + away * 650f
                bot.currentAngle = atan2(away.y, away.x)
                bot.targetAngle = bot.currentAngle
            }
        }
        soundManager.playPotion()
    }

    fun declineRevive() {
        isRevivePromptActive = false
        isGameOver = true
    }

    fun getLiveLeaderboard(): List<LeaderboardEntry> {
        val all = ArrayList<Worm>()
        if (playerWorm.isAlive) all.add(playerWorm)
        all.addAll(botWorms)
        all.sortByDescending { it.score }

        val result = ArrayList<LeaderboardEntry>()
        val topCount = min(10, all.size)
        var playerIncluded = false

        for (i in 0 until topCount) {
            val w = all[i]
            if (w.isPlayer) playerIncluded = true
            result.add(
                LeaderboardEntry(
                    rank = i + 1,
                    name = w.name,
                    score = w.score,
                    kills = w.kills,
                    countryCode = if (w.isPlayer) "ID" else countryCodes[(w.id.hashCode() and 0x7FFFFFFF) % countryCodes.size],
                    isPlayer = w.isPlayer
                )
            )
        }

        if (!playerIncluded && playerWorm.isAlive) {
            val playerRank = all.indexOfFirst { it.isPlayer } + 1
            if (playerRank > 0) {
                result.add(
                    LeaderboardEntry(
                        rank = playerRank,
                        name = playerWorm.name,
                        score = playerWorm.score,
                        kills = playerWorm.kills,
                        countryCode = "ID",
                        isPlayer = true
                    )
                )
            }
        }

        return result
    }

    // Current sector name for radar HUD
    fun getPlayerSectorName(): String {
        val pos = playerWorm.headPosition
        val dist = pos.length()
        return when {
            dist < arenaRadius * 0.25f -> "CORE ARENA"
            pos.x >= 0 && pos.y >= 0 -> "SECTOR ALPHA"
            pos.x < 0 && pos.y >= 0 -> "SECTOR BRAVO"
            pos.x < 0 && pos.y < 0 -> "SECTOR CHARLIE"
            else -> "SECTOR DELTA"
        }
    }
}
