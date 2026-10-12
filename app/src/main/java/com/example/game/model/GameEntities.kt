package com.example.game.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class FoodType {
    APPLE,
    WATERMELON,
    STRAWBERRY,
    DONUT,
    BURGER,
    CAKE,
    POTION,
    MASS_DROP
}

enum class PotionType(val displayName: String, val durationSec: Float, val colorHex: Long) {
    SPEED_BOOST("Turbo 2X", 10f, 0xFF00E676),
    MULTIPLIER_2X("Score x2", 15f, 0xFFFFD700),
    MAGNET("Magnet", 12f, 0xFF00B0FF),
    SHIELD("Pelindung", 8f, 0xFFAA00FF),
    RADAR("Radar Zoom", 14f, 0xFFFF4081)
}

data class Food(
    val id: String = UUID.randomUUID().toString(),
    var position: Vector2D,
    val type: FoodType,
    val potionType: PotionType? = null,
    val value: Int = 10,
    val radius: Float = 14f,
    val primaryColor: Color = Color(0xFFFF5252),
    val secondaryColor: Color = Color(0xFFFFD740),
    var pulsePhase: Float = 0f
)

enum class PatternType {
    SOLID,
    STRIPES,
    GRADIENT,
    RAINBOW,
    NEON_GLOW,
    CANDY_SWIRL,
    DRAGON_SCALES,
    POLKA_DOTS
}

data class Skin(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val pattern: PatternType,
    val price: Int = 0,
    val isUnlocked: Boolean = false,
    val glowColor: Color = Color.Transparent
)

enum class HatType {
    NONE,
    CROWN,
    SUNGLASSES,
    PARTY_HAT,
    BASEBALL_CAP,
    CAT_EARS,
    VIKING_HORNS,
    ANGEL_HALO,
    PIRATE_HAT,
    NINJA_HEADBAND,
    WIZARD_HAT,
    DEVIL_HORNS,
    COWBOY_HAT,
    CHEF_HAT
}

data class Hat(
    val id: String,
    val name: String,
    val type: HatType,
    val price: Int = 0,
    val tag: String = "BASIC",
    val isUnlocked: Boolean = false
)

enum class FaceType {
    HAPPY_SMILE,
    OPEN_MOUTH,
    SILLY_TONGUE,
    COOL_GRIN
}

data class Face(
    val id: String,
    val name: String,
    val type: FaceType,
    val price: Int = 0,
    val tag: String = "EXPRESSION",
    val isUnlocked: Boolean = false
)

data class ActivePotion(
    val type: PotionType,
    var remainingSeconds: Float
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val score: Int,
    val kills: Int = 0,
    val countryCode: String = "ID",
    val isPlayer: Boolean = false
)

data class GameRoom(
    val id: String,
    val name: String,
    val region: String,
    val pingMs: Int,
    val playersCount: Int,
    val maxPlayers: Int = 50,
    val badge: String = "NORMAL"
)

data class KillNotice(
    val id: String = UUID.randomUUID().toString(),
    val victimName: String,
    val bonusCoins: Int = 50,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeathParticle(
    var pos: Vector2D,
    var velocity: Vector2D,
    val color: Color,
    var radius: Float,
    var life: Float = 0.6f,
    val maxLife: Float = 0.6f
)

data class DeathGhostEffect(
    val segments: List<Vector2D>,
    val color: Color,
    val headRadius: Float,
    var life: Float = 0.55f,
    val maxLife: Float = 0.55f
)

data class DailyReward(
    val day: Int,
    val title: String,
    val coins: Int,
    val skinId: String? = null,
    val hatId: String? = null,
    val description: String
)

