package com.yeferson.videojuegolab13.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.yeferson.videojuegolab13.game.FLOATING_TEXT_LIFE
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.Meteor
import com.yeferson.videojuegolab13.game.MeteorKind
import com.yeferson.videojuegolab13.game.PowerUp
import com.yeferson.videojuegolab13.game.PowerUpType
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Cobalto
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Mostaza
import com.yeferson.videojuegolab13.ui.theme.Noche
import com.yeferson.videojuegolab13.ui.theme.Turquesa
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val HuesoSombra = Color(0xFFCDBD9C)

private enum class ShipMood { NORMAL, SHIELDED, POWERED, CRITICAL }

private class Crater(val center: Offset, val radius: Float)

// Figuras que se crean una sola vez y se reutilizan en cada fotograma
private class WorldArt {
    val meteorShapes: List<Path>
    val meteorCraters: List<List<Crater>>

    init {
        val random = Random(21)
        meteorShapes = List(GameEngine.METEOR_SHAPES) {
            Path().apply {
                val points = 9 + random.nextInt(4)
                for (index in 0 until points) {
                    val angle = index / points.toFloat() * 6.2832f
                    val radius = 0.8f + random.nextFloat() * 0.24f
                    val x = cos(angle) * radius
                    val y = sin(angle) * radius
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
        meteorCraters = List(GameEngine.METEOR_SHAPES) {
            List(3) {
                val angle = random.nextFloat() * 6.2832f
                val distance = random.nextFloat() * 0.45f
                Crater(
                    Offset(cos(angle) * distance - 0.08f, sin(angle) * distance - 0.08f),
                    0.1f + random.nextFloat() * 0.13f
                )
            }
        }
    }

    val hull = Path().apply {
        moveTo(0f, -30f)
        cubicTo(8f, -20f, 11f, -6f, 10f, 12f)
        lineTo(6f, 22f)
        lineTo(-6f, 22f)
        lineTo(-10f, 12f)
        cubicTo(-11f, -6f, -8f, -20f, 0f, -30f)
        close()
    }
    val hullShade = Path().apply {
        moveTo(0f, -30f)
        cubicTo(8f, -20f, 11f, -6f, 10f, 12f)
        lineTo(6f, 22f)
        lineTo(0f, 22f)
        close()
    }
    val wings = Path().apply {
        moveTo(-9f, -3f)
        lineTo(-30f, 17f)
        lineTo(-30f, 26f)
        lineTo(-8f, 16f)
        close()
        moveTo(9f, -3f)
        lineTo(30f, 17f)
        lineTo(30f, 26f)
        lineTo(8f, 16f)
        close()
    }
    val cockpit = Path().apply {
        moveTo(0f, -18f)
        cubicTo(5f, -11f, 5f, -3f, 0f, 1f)
        cubicTo(-5f, -3f, -5f, -11f, 0f, -18f)
        close()
    }
    val flame = Path().apply {
        moveTo(-5f, 0f)
        lineTo(5f, 0f)
        lineTo(0f, 1f)
        close()
    }
    val warningMark = Path().apply {
        moveTo(0f, -11f)
        lineTo(11f, 9f)
        lineTo(-11f, 9f)
        close()
    }
    val shieldIcon = Path().apply {
        moveTo(0f, -9f)
        lineTo(8f, -6f)
        lineTo(7f, 3f)
        lineTo(0f, 10f)
        lineTo(-7f, 3f)
        lineTo(-8f, -6f)
        close()
    }
    val hourglassIcon = Path().apply {
        moveTo(-7f, -9f)
        lineTo(7f, -9f)
        lineTo(-7f, 9f)
        lineTo(7f, 9f)
        close()
    }
    val warningDash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 17f
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
}

@Composable
fun GameWorld(engine: GameEngine, modifier: Modifier = Modifier) {
    val art = remember { WorldArt() }

    // La llama del motor cambia de largo sin detenerse
    val flameLength by rememberInfiniteTransition(label = "motor").animateFloat(
        initialValue = 14f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "llama"
    )

    // updateTransition coordina varias propiedades de la nave con un solo estado
    val mood = when {
        engine.hasShield -> ShipMood.SHIELDED
        engine.activePower == PowerUpType.TRIPLE -> ShipMood.POWERED
        engine.hull == 1 -> ShipMood.CRITICAL
        else -> ShipMood.NORMAL
    }
    val shipTransition = updateTransition(targetState = mood, label = "estadoNave")
    val auraColor by shipTransition.animateColor(
        transitionSpec = { tween(350) },
        label = "colorAura"
    ) { state ->
        when (state) {
            ShipMood.NORMAL -> Turquesa.copy(alpha = 0f)
            ShipMood.SHIELDED -> Turquesa
            ShipMood.POWERED -> Mostaza
            ShipMood.CRITICAL -> Bermellon
        }
    }
    val auraRadius by shipTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.42f, stiffness = 280f) },
        label = "radioAura"
    ) { state ->
        when (state) {
            ShipMood.NORMAL -> 22f
            ShipMood.SHIELDED -> 48f
            ShipMood.POWERED -> 42f
            ShipMood.CRITICAL -> 40f
        }
    }
    val shipScale by shipTransition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.5f, stiffness = 300f) },
        label = "escalaNave"
    ) { state ->
        when (state) {
            ShipMood.POWERED -> 1.16f
            ShipMood.CRITICAL -> 0.92f
            else -> 1f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Leer frame hace que el Canvas se redibuje sin recomponer la pantalla
        engine.frame
        scale(density, density, Offset.Zero) {
            drawWarnings(engine, art)
            engine.powerUps.forEach { drawPowerUp(it, art) }
            engine.meteors.forEach { drawMeteor(it, art) }
            drawBullets(engine)
            drawParticles(engine)
            drawWaves(engine)
            drawPulse(engine)
            if (engine.shipVisible) {
                drawAura(engine, auraColor, auraRadius)
                drawShip(engine, art, flameLength, shipScale)
            }
            drawTexts(engine, art)
        }
    }
}

private fun DrawScope.drawAura(engine: GameEngine, color: Color, radius: Float) {
    if (color.alpha <= 0.01f) return
    val center = Offset(engine.shipX, engine.shipDrawY)
    val beat = radius + sin(engine.clock * 6f) * 3f
    drawCircle(color.copy(alpha = color.alpha * 0.16f), beat, center)
    drawCircle(color.copy(alpha = color.alpha * 0.85f), beat, center, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawShip(
    engine: GameEngine,
    art: WorldArt,
    flameLength: Float,
    shipScale: Float
) {
    // Parpadea mientras la nave es invulnerable después de un impacto
    if (engine.invulnerableTime > 0f && sin(engine.clock * 34f) > 0.2f) return

    withTransform({
        translate(engine.shipX, engine.shipDrawY)
        rotate(engine.shipTilt * 16f, Offset.Zero)
        scale((1f - abs(engine.shipTilt) * 0.14f) * shipScale, shipScale, Offset.Zero)
    }) {
        withTransform({
            translate(0f, 25f)
            scale(1f, flameLength, Offset.Zero)
        }) {
            drawPath(art.flame, Bermellon)
        }
        withTransform({
            translate(0f, 25f)
            scale(0.55f, flameLength * 0.6f, Offset.Zero)
        }) {
            drawPath(art.flame, Mostaza)
        }
        drawPath(art.wings, Bermellon)
        drawRect(Mostaza, Offset(-32f, 14f), Size(4f, 14f))
        drawRect(Mostaza, Offset(28f, 14f), Size(4f, 14f))
        drawRect(Noche, Offset(-6f, 21f), Size(12f, 5f))
        drawPath(art.hull, Hueso)
        drawPath(art.hullShade, HuesoSombra)
        drawRect(Bermellon, Offset(-10.4f, 6f), Size(20.8f, 4f))
        drawPath(art.cockpit, Cobalto)
        drawLine(Turquesa, Offset(-1.5f, -13f), Offset(-1.5f, -6f), 1.6f, StrokeCap.Round)
    }
}

private fun DrawScope.drawMeteor(meteor: Meteor, art: WorldArt) {
    val shape = art.meteorShapes[meteor.shape]
    if (meteor.kind == MeteorKind.MAGMA) {
        drawCircle(Bermellon.copy(alpha = 0.28f), meteor.radius * 1.8f, Offset(meteor.x, meteor.y))
    }
    withTransform({
        translate(meteor.x, meteor.y)
        rotate(meteor.rotation, Offset.Zero)
        scale(meteor.radius, meteor.radius, Offset.Zero)
    }) {
        drawPath(shape, Color(meteor.shadow))
        withTransform({
            translate(-0.08f, -0.08f)
            scale(0.84f, 0.84f, Offset.Zero)
        }) {
            drawPath(shape, Color(meteor.color))
        }
        art.meteorCraters[meteor.shape].forEach { crater ->
            drawCircle(Color(meteor.shadow), crater.radius, crater.center)
        }
        if (meteor.hitFlash > 0f) drawPath(shape, Hueso.copy(alpha = meteor.hitFlash))
    }
}

private fun DrawScope.drawBullets(engine: GameEngine) {
    engine.bullets.forEach { bullet ->
        drawLine(
            color = Mostaza,
            start = Offset(bullet.x, bullet.y),
            end = Offset(bullet.x - bullet.vx * 0.024f, bullet.y - bullet.vy * 0.024f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawParticles(engine: GameEngine) {
    engine.particles.forEach { particle ->
        val progress = (particle.life / particle.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color(particle.color).copy(alpha = progress),
            radius = particle.size * (0.4f + 0.6f * progress),
            center = Offset(particle.x, particle.y)
        )
    }
}

private fun DrawScope.drawWaves(engine: GameEngine) {
    engine.waves.forEach { wave ->
        val progress = (wave.life / wave.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color(wave.color).copy(alpha = progress * 0.8f),
            radius = wave.maxRadius * (1f - progress * progress),
            center = Offset(wave.x, wave.y),
            style = Stroke(width = 1f + progress * 5f)
        )
    }
}

private fun DrawScope.drawPulse(engine: GameEngine) {
    if (engine.pulseRadius < 0f) return
    val center = Offset(engine.pulseX, engine.pulseY)
    drawCircle(Hueso.copy(alpha = 0.12f), engine.pulseRadius, center)
    drawCircle(Hueso, engine.pulseRadius, center, style = Stroke(width = 10f))
    drawCircle(Mostaza, (engine.pulseRadius - 16f).coerceAtLeast(0f), center, style = Stroke(width = 5f))
}

private fun DrawScope.drawPowerUp(powerUp: PowerUp, art: WorldArt) {
    val beat = 1f + sin(powerUp.age * 6f) * 0.1f
    val color = when (powerUp.type) {
        PowerUpType.SHIELD -> Turquesa
        PowerUpType.TRIPLE -> Mostaza
        PowerUpType.SLOW -> Hueso
    }
    withTransform({
        translate(powerUp.x, powerUp.y)
        scale(beat, beat, Offset.Zero)
    }) {
        drawCircle(color.copy(alpha = 0.3f), 24f, Offset.Zero)
        drawCircle(color, 18f, Offset.Zero)
        when (powerUp.type) {
            PowerUpType.SHIELD -> drawPath(art.shieldIcon, Noche)
            PowerUpType.TRIPLE -> for (side in -1..1) {
                drawLine(Noche, Offset(side * 6f, 8f), Offset(side * 6f, -8f), 3.4f, StrokeCap.Round)
            }
            PowerUpType.SLOW -> drawPath(art.hourglassIcon, Noche)
        }
    }
}

private fun DrawScope.drawWarnings(engine: GameEngine, art: WorldArt) {
    if (sin(engine.clock * 20f) < -0.25f) return
    engine.warnings.forEach { warning ->
        drawLine(
            color = Bermellon.copy(alpha = 0.7f),
            start = Offset(warning.x, 66f),
            end = Offset(warning.x, engine.worldHeight * 0.4f),
            strokeWidth = 2.5f,
            pathEffect = art.warningDash
        )
        withTransform({ translate(warning.x, 50f) }) {
            drawPath(art.warningMark, Bermellon)
            drawLine(Hueso, Offset(0f, -4f), Offset(0f, 2f), 2.4f, StrokeCap.Round)
            drawCircle(Hueso, 1.4f, Offset(0f, 6f))
        }
    }
}

private fun DrawScope.drawTexts(engine: GameEngine, art: WorldArt) {
    drawIntoCanvas { canvas ->
        engine.texts.forEach { text ->
            val alpha = (text.life / FLOATING_TEXT_LIFE).coerceIn(0f, 1f)
            art.textPaint.color = Color(text.color).copy(alpha = alpha).toArgb()
            canvas.nativeCanvas.drawText(text.text, text.x, text.y, art.textPaint)
        }
    }
}
