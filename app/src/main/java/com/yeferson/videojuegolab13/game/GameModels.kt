package com.yeferson.videojuegolab13.game

// Estados por los que pasa la partida
enum class GamePhase { MENU, COUNTDOWN, PLAYING, PAUSED, DESTROYED, GAME_OVER }

// Tamaños de meteorito: los grandes resisten más y dan más puntos
enum class MeteorSize(val radius: Float, val hp: Int, val points: Int, val charge: Float) {
    SMALL(15f, 1, 10, 0.05f),
    MEDIUM(27f, 3, 25, 0.08f),
    LARGE(44f, 6, 50, 0.14f)
}

enum class MeteorKind { ROCK, MAGMA }

enum class PowerUpType { SHIELD, TRIPLE, SLOW }

class Meteor(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: MeteorSize,
    val kind: MeteorKind,
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

class MagmaWarning(val x: Float) {
    var timeLeft = MAGMA_WARNING_TIME
}

const val FLOATING_TEXT_LIFE = 0.9f
const val MAGMA_WARNING_TIME = 1.0f

// Cada sector cambia los colores del cielo y de los meteoritos
data class Sector(
    val name: String,
    val skyTop: Int,
    val skyBottom: Int,
    val rock: Int,
    val rockShadow: Int
)

val Sectors = listOf(
    Sector("Órbita baja", 0xFF1B2A8A.toInt(), 0xFF0B1248.toInt(), 0xFFB9A58A.toInt(), 0xFF6F5B52.toInt()),
    Sector("Cinturón de hielo", 0xFF0E5A78.toInt(), 0xFF07263F.toInt(), 0xFFA9DCE3.toInt(), 0xFF4C7F99.toInt()),
    Sector("Nebulosa carmesí", 0xFF7A1C3D.toInt(), 0xFF2A0B2B.toInt(), 0xFFE0A58F.toInt(), 0xFF84424C.toInt()),
    Sector("Zona ígnea", 0xFF8A3A12.toInt(), 0xFF2B0F12.toInt(), 0xFFD9B26A.toInt(), 0xFF7D4B24.toInt()),
    Sector("Vacío profundo", 0xFF2B2350.toInt(), 0xFF0B0A1F.toInt(), 0xFFB7B0D6.toInt(), 0xFF5A5280.toInt())
)

fun sectorAt(index: Int): Sector = Sectors[index % Sectors.size]
