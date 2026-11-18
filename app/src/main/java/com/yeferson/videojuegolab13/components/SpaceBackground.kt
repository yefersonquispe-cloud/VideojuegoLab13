package com.yeferson.videojuegolab13.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.sectorAt
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private const val RAY_COUNT = 12
private const val TWO_PI = 6.2832f
private val FranjaPlaneta = Color(0xFFB02E22)

private class Star(val x: Float, val y: Float, val layer: Int, val phase: Float)

@Composable
fun SpaceBackground(engine: GameEngine, modifier: Modifier = Modifier) {
    val inMenu = engine.phase == GamePhase.MENU
    val sector = sectorAt(engine.sectorIndex)

    // El cielo cambia suavemente de color al entrar a cada sector
    val skyTop by animateColorAsState(
        targetValue = Color(sector.skyTop),
        animationSpec = tween(1800, easing = FastOutSlowInEasing),
        label = "cieloSuperior"
    )
    val skyBottom by animateColorAsState(
        targetValue = Color(sector.skyBottom),
        animationSpec = tween(1800, easing = FastOutSlowInEasing),
        label = "cieloInferior"
    )

    // Animaciones que se repiten sin parar: brillo de estrellas y giro de los rayos
    val transition = rememberInfiniteTransition(label = "espacio")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "brillo"
    )
    val rayAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(70000, easing = LinearEasing)),
        label = "rayos"
    )

    val raysAlpha by animateFloatAsState(
        targetValue = if (inMenu) 1f else 0f,
        animationSpec = tween(900),
        label = "opacidadRayos"
    )

    // El planeta baja y sale de la pantalla cuando la nave despega
    val planetDrop by animateDpAsState(
        targetValue = if (inMenu) 0.dp else 520.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow),
        label = "caidaPlaneta"
    )

    val stars = remember {
        val random = Random(13)
        List(120) {
            Star(random.nextFloat(), random.nextFloat(), random.nextInt(3), random.nextFloat() * TWO_PI)
        }
    }
    val rayPath = remember { Path() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))
            if (raysAlpha > 0.01f) drawRays(rayPath, rayAngle, raysAlpha)
            drawStars(stars, engine.scroll, engine.scrollSpeed, twinkle)
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 170.dp + planetDrop)
                .size(440.dp)
        ) {
            drawPlanet()
        }
    }
}

// Rayos al estilo de los carteles antiguos, centrados en la nave del menú
private fun DrawScope.drawRays(path: Path, angle: Float, alpha: Float) {
    val center = Offset(size.width * 0.62f, size.height * 0.5f)
    val radius = hypot(size.width, size.height)
    val wedge = TWO_PI / (RAY_COUNT * 2)
    rotate(angle, center) {
        for (index in 0 until RAY_COUNT) {
            val start = index * wedge * 2
            path.rewind()
            path.moveTo(center.x, center.y)
            path.lineTo(center.x + cos(start) * radius, center.y + sin(start) * radius)
            path.lineTo(center.x + cos(start + wedge) * radius, center.y + sin(start + wedge) * radius)
            path.close()
            drawPath(path, Hueso.copy(alpha = 0.075f * alpha))
        }
    }
}

private fun DrawScope.drawStars(stars: List<Star>, scroll: Float, speed: Float, twinkle: Float) {
    val streak = ((speed - 180f) * 0.14f).coerceAtLeast(0f) * density
    for (star in stars) {
        val depth = 0.3f + star.layer * 0.35f
        val travel = scroll * density * depth
        val y = (star.y * size.height + travel) % size.height
        val x = star.x * size.width
        val shine = 0.55f + 0.45f * sin(twinkle + star.phase)
        val radius = (0.8f + star.layer * 0.7f) * density
        val color = Hueso.copy(alpha = (0.3f + 0.7f * shine) * depth.coerceAtMost(1f))

        if (streak > 1f) {
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x, y - streak * depth),
                strokeWidth = radius,
                cap = StrokeCap.Round
            )
        } else if (star.layer == 2) {
            // Las estrellas cercanas tienen forma de cruz
            val arm = radius * (2f + shine * 2f)
            drawLine(color, Offset(x - arm, y), Offset(x + arm, y), density * 0.9f, StrokeCap.Round)
            drawLine(color, Offset(x, y - arm), Offset(x, y + arm), density * 0.9f, StrokeCap.Round)
        } else {
            drawCircle(color, radius, Offset(x, y))
        }
    }
}

private fun DrawScope.drawPlanet() {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.42f
    val ringSize = Size(radius * 2.9f, radius * 0.72f)
    val ringTopLeft = Offset(center.x - ringSize.width / 2f, center.y - ringSize.height / 2f)
    val ringStroke = Stroke(width = radius * 0.07f)

    rotate(-18f, center) {
        drawArc(Hueso, 180f, 180f, false, ringTopLeft, ringSize, style = ringStroke)
    }

    val disc = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center, radius))
    }
    clipPath(disc) {
        drawRect(Bermellon)
        // Franjas planas del planeta
        rotate(-18f, center) {
            drawRect(
                FranjaPlaneta,
                Offset(0f, center.y - radius * 0.62f),
                Size(size.width, radius * 0.2f)
            )
            drawRect(
                Hueso,
                Offset(0f, center.y - radius * 0.28f),
                Size(size.width, radius * 0.08f)
            )
            drawRect(
                FranjaPlaneta,
                Offset(0f, center.y + radius * 0.12f),
                Size(size.width, radius * 0.34f)
            )
        }
        // Sombra en forma de media luna
        drawCircle(
            Color(0x66140A2E),
            radius * 1.25f,
            Offset(center.x + radius * 0.62f, center.y + radius * 0.5f)
        )
    }

    rotate(-18f, center) {
        drawArc(Hueso, 0f, 180f, false, ringTopLeft, ringSize, style = ringStroke)
    }
}
