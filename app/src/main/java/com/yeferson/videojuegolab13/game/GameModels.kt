package com.yeferson.videojuegolab13.game

enum class GamePhase { MENU, COUNTDOWN, PLAYING, PAUSED, DESTROYED, GAME_OVER }

enum class MeteorSize(val radius: Float, val hp: Int, val points: Int, val charge: Float) {
    SMALL(15f, 1, 10, 0.05f),
    MEDIUM(27f, 3, 25, 0.08f),
    LARGE(44f, 6, 50, 0.14f)
}

enum class PowerUpType { SHIELD, TRIPLE }

class Meteor(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: MeteorSize,
    val shape: Int,
    val spin: Float,
    val color: Int,
    val shadow: Int
) {
    var rotation = 0f
    var hp = size.hp
    var hitFlash = 0f
    val radius get() = size.radius
}

class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float)

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val maxLife: Float,
    val size: Float,
    val color: Int
) {
    var life = maxLife
}

class Shockwave(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val maxLife: Float,
    val color: Int
) {
    var life = maxLife
}

class PowerUp(var x: Float, var y: Float, val type: PowerUpType) {
    val originX = x
    var age = 0f
}

class FloatingText(var x: Float, var y: Float, val text: String, val color: Int) {
    var life = FLOATING_TEXT_LIFE
}

const val FLOATING_TEXT_LIFE = 0.9f
