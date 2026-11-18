package com.yeferson.videojuegolab13.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.yeferson.videojuegolab13.components.GameHud
import com.yeferson.videojuegolab13.components.GameOverlays
import com.yeferson.videojuegolab13.components.GameWorld
import com.yeferson.videojuegolab13.components.SpaceBackground
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.ui.theme.Noche

@Composable
fun GameScreen() {
    val engine = remember { GameEngine() }
    val density = LocalDensity.current.density

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
        SpaceBackground(engine = engine)
        GameWorld(engine = engine)
        GameHud(engine = engine)
        GameOverlays(engine = engine)
    }
}
