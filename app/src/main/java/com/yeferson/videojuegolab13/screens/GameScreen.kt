package com.yeferson.videojuegolab13.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.yeferson.videojuegolab13.components.GameAlerts
import com.yeferson.videojuegolab13.components.GameHud
import com.yeferson.videojuegolab13.components.GameOverlays
import com.yeferson.videojuegolab13.components.GameWorld
import com.yeferson.videojuegolab13.components.SpaceBackground
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.RecordStore
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Noche
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameScreen() {
    val engine = remember { GameEngine() }
    val density = LocalDensity.current.density
    val haptics = LocalHapticFeedback.current

    val context = LocalContext.current
    val recordStore = remember { RecordStore(context.applicationContext) }
    LaunchedEffect(engine) { engine.loadBestScore(recordStore.load()) }
    LaunchedEffect(engine.bestScore) {
        if (engine.bestScore > 0) recordStore.save(engine.bestScore)
    }

    val shakeX = remember { Animatable(0f) }
    val worldZoom = remember { Animatable(1f) }
    val hitFlash = remember { Animatable(0f) }
    val pulseFlash = remember { Animatable(0f) }

    BackHandler(enabled = engine.phase == GamePhase.PLAYING || engine.phase == GamePhase.PAUSED) {
        if (engine.phase == GamePhase.PLAYING) engine.pause() else engine.resume()
    }

    LaunchedEffect(engine) {
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                engine.step((now - previous) / 1_000_000_000f)
                previous = now
            }
        }
    }

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
        hitFlash.snapTo(0.55f)
        hitFlash.animateTo(0f, tween(520))
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Noche)
            .onSizeChanged { engine.resize(it.width / density, it.height / density) }
            .pointerInput(engine) {
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
                    val shakeZoom = abs(shakeX.value) / 200f
                    scaleX = worldZoom.value + shakeZoom
                    scaleY = worldZoom.value + shakeZoom
                }
        ) {
            SpaceBackground(engine = engine)
            GameWorld(engine = engine)
        }

        ScreenFlash(color = Bermellon, alpha = { hitFlash.value })
        ScreenFlash(color = Hueso, alpha = { pulseFlash.value })

        GameHud(engine = engine)
        GameAlerts(engine = engine)
        GameOverlays(engine = engine)
    }
}

@Composable
private fun ScreenFlash(color: Color, alpha: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() }
            .background(color)
    )
}
