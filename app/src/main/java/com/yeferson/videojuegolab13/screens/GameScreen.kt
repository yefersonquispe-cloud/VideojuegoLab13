package com.yeferson.videojuegolab13.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.yeferson.videojuegolab13.components.SpaceBackground
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.ui.theme.Noche

@Composable
fun GameScreen() {
    val engine = remember { GameEngine() }
    val density = LocalDensity.current.density

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
    ) {
        SpaceBackground(engine = engine)
    }
}
