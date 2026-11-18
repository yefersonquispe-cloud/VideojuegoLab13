package com.yeferson.videojuegolab13.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.PowerUpType
import com.yeferson.videojuegolab13.game.sectorAt
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Mostaza
import com.yeferson.videojuegolab13.ui.theme.Noche
import com.yeferson.videojuegolab13.ui.theme.Turquesa

@Composable
fun GameAlerts(engine: GameEngine, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        SectorBanner(
            engine = engine,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 124.dp)
        )
        ComboBadge(
            engine = engine,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 104.dp)
        )
        PowerStatus(
            engine = engine,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp)
        )
    }
}

// Aviso que entra por la izquierda y sale por la derecha al cambiar de sector
@Composable
private fun SectorBanner(engine: GameEngine, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = engine.sectorBannerVisible && engine.phase == GamePhase.PLAYING,
        modifier = modifier,
        enter = slideInHorizontally(
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
        ) { -it * 2 } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(380)) { it * 2 } + fadeOut(tween(380))
    ) {
        Column(
            modifier = Modifier
                .background(Hueso, CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Sector ${engine.sectorIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = Bermellon
            )
            Text(
                text = sectorAt(engine.sectorIndex).name,
                style = MaterialTheme.typography.titleLarge,
                color = Noche
            )
        }
    }
}

// El multiplicador aparece con rebote y cambia de color al subir
@Composable
private fun ComboBadge(engine: GameEngine, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = when {
            engine.combo >= 4 -> Bermellon
            engine.combo == 3 -> Mostaza
            else -> Hueso
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "colorCombo"
    )

    AnimatedVisibility(
        visible = engine.combo > 1 && engine.phase == GamePhase.PLAYING,
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.42f, stiffness = 320f)) + fadeIn(),
        exit = scaleOut(tween(220), targetScale = 0.5f) + fadeOut(tween(220))
    ) {
        AnimatedContent(
            targetState = maxOf(engine.combo, 2),
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.4f, stiffness = 380f), initialScale = 1.7f) +
                    fadeIn(tween(100))) togetherWith fadeOut(tween(100))
            },
            label = "valorCombo"
        ) { value ->
            Text(
                text = "x$value",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                color = color
            )
        }
    }
}

@Composable
private fun PowerStatus(engine: GameEngine, modifier: Modifier = Modifier) {
    // Guarda el último poder para mostrar su nombre mientras la animación de salida termina
    var lastPower by remember { mutableStateOf(PowerUpType.TRIPLE) }
    SideEffect { engine.activePower?.let { lastPower = it } }
    val shownPower = engine.activePower ?: lastPower
    val powerColor = if (shownPower == PowerUpType.TRIPLE) Mostaza else Hueso

    val progress by animateFloatAsState(
        targetValue = engine.powerFraction,
        animationSpec = tween(160, easing = LinearEasing),
        label = "tiempoPoder"
    )

    Column(
        modifier = modifier.width(176.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = engine.hasShield,
            enter = slideInHorizontally(spring(dampingRatio = 0.65f)) { -it } + fadeIn(),
            exit = slideOutHorizontally(tween(260)) { -it } + fadeOut(tween(260))
        ) {
            Text(
                text = "Escudo activo",
                modifier = Modifier
                    .background(Turquesa, CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Noche
            )
        }

        AnimatedVisibility(
            visible = engine.activePower != null,
            enter = slideInVertically(spring(dampingRatio = 0.6f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(260))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Noche.copy(alpha = 0.72f))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (shownPower == PowerUpType.TRIPLE) "Disparo triple" else "Tiempo lento",
                    style = MaterialTheme.typography.labelLarge,
                    color = powerColor
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Hueso.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(powerColor)
                    )
                }
            }
        }
    }
}
