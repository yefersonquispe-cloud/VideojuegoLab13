package com.yeferson.videojuegolab13.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.yeferson.videojuegolab13.components.GameAlerts
import com.yeferson.videojuegolab13.components.GameHud
import com.yeferson.videojuegolab13.components.GameOverlays
import com.yeferson.videojuegolab13.components.GameWorld
import com.yeferson.videojuegolab13.components.SpaceBackground
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.PowerUpType
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Noche
import com.yeferson.videojuegolab13.ui.theme.Turquesa
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameScreen() {
    val engine = remember { GameEngine() }
    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current

    // Valores controlados a mano con Animatable para reaccionar a cada evento
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val worldZoom = remember { Animatable(1f) }
    val hitFlash = remember { Animatable(0f) }
    val shieldFlash = remember { Animatable(0f) }
    val pulseFlash = remember { Animatable(0f) }

    // El botón atrás pausa la partida o la continúa
    BackHandler(enabled = engine.phase == GamePhase.PLAYING || engine.phase == GamePhase.PAUSED) {
        if (engine.phase == GamePhase.PLAYING) engine.pause() else engine.resume()
    }

    // Bucle principal: avanza el juego una vez por cada fotograma de la pantalla
    LaunchedEffect(engine) {
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                engine.step((now - previous) / 1_000_000_000f)
                previous = now
            }
        }
    }

    // Impacto: sacude la pantalla con keyframes y lanza un destello rojo
    LaunchedEffect(engine.hitEvents) {
        if (engine.hitEvents == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 460
                    20f at 40
                    -17f at 100
                    13f at 170
                    -8f at 250
                    4f at 340
                }
            )
        }
        launch {
            shakeY.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -12f at 50
                    10f at 120
                    -6f at 210
                    3f at 300
                }
            )
        }
        hitFlash.snapTo(0.55f)
        hitFlash.animateTo(0f, tween(520))
    }

    // Escudo: destello turquesa al absorber un golpe
    LaunchedEffect(engine.shieldEvents) {
        if (engine.shieldEvents == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        shieldFlash.snapTo(0.4f)
        shieldFlash.animateTo(0f, tween(420))
    }

    // Pulso: destello claro y un pequeño acercamiento con rebote
    LaunchedEffect(engine.pulseEvents) {
        if (engine.pulseEvents == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            worldZoom.snapTo(1.07f)
            worldZoom.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 180f))
        }
        pulseFlash.snapTo(0.75f)
        pulseFlash.animateTo(0f, tween(620))
    }

    // El mundo se desenfoca detrás de las pantallas de pausa y fin de partida
    val blurRadius by animateDpAsState(
        targetValue = if (engine.phase == GamePhase.PAUSED || engine.phase == GamePhase.GAME_OVER) 8.dp else 0.dp,
        animationSpec = tween(450),
        label = "desenfoque"
    )

    // Tinte turquesa mientras el tiempo va lento
    val slowTint by animateColorAsState(
        targetValue = if (engine.activePower == PowerUpType.SLOW) Turquesa.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(500),
        label = "tinteTiempoLento"
    )

    // Borde rojo que late cuando solo queda una vida
    val dangerLevel by animateFloatAsState(
        targetValue = if (engine.hull == 1 && engine.phase == GamePhase.PLAYING) 1f else 0f,
        animationSpec = tween(600),
        label = "nivelPeligro"
    )
    val dangerBeat by rememberInfiniteTransition(label = "peligro").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "latidoPeligro"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Noche)
            .onSizeChanged { engine.resize(it.width / density, it.height / density) }
            .pointerInput(engine) {
                // La nave sigue al dedo que más se mueve, incluso al tocar otro botón
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val drag = event.changes
                            .filter { it.pressed && it.previousPressed }
                            .maxByOrNull { it.positionChangeIgnoreConsumed().getDistanceSquared() }
                            ?.positionChangeIgnoreConsumed()
                        if (drag != null) engine.moveShipBy(drag.x / density, drag.y / density)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeX.value * density
                    translationY = shakeY.value * density
                    // Aumenta un poco la escala para que no se vean los bordes al sacudir
                    val shakeZoom = (abs(shakeX.value) + abs(shakeY.value)) / 200f
                    scaleX = worldZoom.value + shakeZoom
                    scaleY = worldZoom.value + shakeZoom
                }
                .blur(blurRadius)
        ) {
            SpaceBackground(engine = engine)
            GameWorld(engine = engine)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(slowTint)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val alpha = dangerLevel * dangerBeat
            if (alpha > 0.01f) {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.55f to Color.Transparent,
                            1f to Bermellon.copy(alpha = 0.6f * alpha)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.62f
                    )
                )
            }
        }
        ScreenFlash(color = Bermellon, alpha = { hitFlash.value })
        ScreenFlash(color = Turquesa, alpha = { shieldFlash.value })
        ScreenFlash(color = Hueso, alpha = { pulseFlash.value })

        GameHud(engine = engine)
        GameAlerts(engine = engine)
        GameOverlays(engine = engine)
    }
}

// Lee la opacidad dentro de graphicsLayer para no recomponer en cada fotograma
@Composable
private fun ScreenFlash(color: Color, alpha: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() }
            .background(color)
    )
}
