package com.yeferson.videojuegolab13.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.game.MAX_HULL
import com.yeferson.videojuegolab13.game.sectorAt
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Mostaza
import com.yeferson.videojuegolab13.ui.theme.Noche
import com.yeferson.videojuegolab13.ui.theme.Turquesa

@Composable
fun GameHud(engine: GameEngine, modifier: Modifier = Modifier) {
    val visible = engine.phase == GamePhase.COUNTDOWN ||
        engine.phase == GamePhase.PLAYING ||
        engine.phase == GamePhase.PAUSED

    // El panel entra desde fuera de la pantalla usando offset
    val topOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else (-170).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "panelSuperior"
    )
    val bottomOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 170.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "panelInferior"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = topOffset),
            verticalAlignment = Alignment.Top
        ) {
            Column {
                RollingScore(score = engine.score)
                Spacer(Modifier.height(8.dp))
                HullBar(hull = engine.hull)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(engine.secondsSurvived),
                        style = MaterialTheme.typography.titleLarge,
                        color = Hueso
                    )
                    Spacer(Modifier.width(12.dp))
                    PauseButton(onClick = engine::pause)
                }
                Text(
                    text = sectorAt(engine.sectorIndex).name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Hueso.copy(alpha = 0.8f)
                )
            }
        }

        PulseButton(
            charge = engine.pulseCharge,
            onClick = { engine.firePulse() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = bottomOffset)
                .padding(bottom = 12.dp)
        )
    }
}

fun formatTime(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)

// Cada dígito del puntaje sube como en un contador mecánico
@Composable
private fun RollingScore(score: Int) {
    Row {
        score.toString().padStart(6, '0').forEach { digit ->
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    (slideInVertically(tween(170)) { it } + fadeIn(tween(170))) togetherWith
                        (slideOutVertically(tween(170)) { -it } + fadeOut(tween(120)))
                },
                label = "digito"
            ) { value ->
                Text(
                    text = value.toString(),
                    modifier = Modifier.width(19.dp),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                    color = Hueso,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HullBar(hull: Int) {
    val activeColor = when (hull) {
        MAX_HULL -> Turquesa
        2 -> Mostaza
        else -> Bermellon
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(MAX_HULL) { index ->
            val active = index < hull
            // Color con spring y ancho con animateDpAsState al perder una vida
            val color by animateColorAsState(
                targetValue = if (active) activeColor else Hueso.copy(alpha = 0.2f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "colorVida"
            )
            val width by animateDpAsState(
                targetValue = if (active) 36.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "anchoVida"
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 11.dp)
                    .background(color, CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
            )
        }
    }
}

@Composable
private fun PauseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .size(46.dp)
            .clip(CutCornerShape(10.dp))
            .background(Noche.copy(alpha = 0.6f))
            .clickable(onClickLabel = "Pausar", onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            Box(
                Modifier
                    .size(width = 5.dp, height = 18.dp)
                    .background(Hueso)
            )
        }
    }
}

@Composable
private fun PulseButton(charge: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ready = charge >= 1f
    val animatedCharge by animateFloatAsState(
        targetValue = charge,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cargaPulso"
    )
    val ringColor by animateColorAsState(
        targetValue = if (ready) Mostaza else Hueso.copy(alpha = 0.75f),
        animationSpec = tween(400),
        label = "colorPulso"
    )
    val fillColor by animateColorAsState(
        targetValue = if (ready) Mostaza else Noche.copy(alpha = 0.6f),
        animationSpec = tween(400),
        label = "fondoPulso"
    )
    val beat by rememberInfiniteTransition(label = "latido").animateFloat(
        initialValue = 1f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaLatido"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "presionPulso"
    )

    Box(
        modifier = modifier
            .size(92.dp)
            .graphicsLayer {
                val scale = pressScale * if (ready) beat else 1f
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = ready,
                onClickLabel = "Lanzar pulso",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            drawCircle(fillColor)
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedCharge,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = if (ready) "Lanzar\npulso" else "${(charge * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = if (ready) Noche else Hueso,
            textAlign = TextAlign.Center
        )
    }
}
