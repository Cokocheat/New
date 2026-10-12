package com.example.game.model

import kotlin.math.*

data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D()

    fun length(): Float = hypot(x, y)
    fun lengthSquared(): Float = x * x + y * y

    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D(1f, 0f)
    }

    fun distanceTo(other: Vector2D): Float = hypot(x - other.x, y - other.y)
    fun distanceSquaredTo(other: Vector2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun angle(): Float = atan2(y, x)

    companion object {
        fun fromAngle(angleRad: Float, length: Float = 1f): Vector2D {
            return Vector2D(cos(angleRad) * length, sin(angleRad) * length)
        }

        fun lerp(a: Vector2D, b: Vector2D, t: Float): Vector2D {
            val clampedT = t.coerceIn(0f, 1f)
            return Vector2D(
                a.x + (b.x - a.x) * clampedT,
                a.y + (b.y - a.y) * clampedT
            )
        }
    }
}
