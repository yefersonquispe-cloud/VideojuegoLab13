package com.yeferson.videojuegolab13.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeferson.videojuegolab13.game.GameEngine
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
        PowerStatus(
            engine = engine,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun PowerStatus(engine: GameEngine, modifier: Modifier = Modifier) {
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
                    text = "Disparo triple",
                    style = MaterialTheme.typography.labelLarge,
                    color = Mostaza
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
                            .background(Mostaza)
                    )
                }
            }
        }
    }
}
