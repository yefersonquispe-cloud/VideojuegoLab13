package com.yeferson.videojuegolab13.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeferson.videojuegolab13.game.GameEngine
import com.yeferson.videojuegolab13.game.GamePhase
import com.yeferson.videojuegolab13.ui.theme.Bermellon
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Mostaza
import com.yeferson.videojuegolab13.ui.theme.Noche
import kotlinx.coroutines.delay

private enum class OverlayKind { MENU, COUNTDOWN, NONE, PAUSE, GAME_OVER }

private val PosterShape = GenericShape { size, _ ->
    moveTo(size.width * 0.05f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width * 0.95f, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun GameOverlays(engine: GameEngine, modifier: Modifier = Modifier) {
    val kind = when (engine.phase) {
        GamePhase.MENU -> OverlayKind.MENU
        GamePhase.COUNTDOWN -> OverlayKind.COUNTDOWN
        GamePhase.PAUSED -> OverlayKind.PAUSE
        GamePhase.GAME_OVER -> OverlayKind.GAME_OVER
        else -> OverlayKind.NONE
    }

    // AnimatedContent cambia entre las pantallas del juego con tiempos personalizados
    AnimatedContent(
        targetState = kind,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(tween(500, delayMillis = 120)) +
                scaleIn(tween(500, delayMillis = 120), initialScale = 0.94f)) togetherWith
                fadeOut(tween(300))
        },
        label = "pantallasDelJuego"
    ) { current ->
        when (current) {
            OverlayKind.MENU -> MenuOverlay(engine)
            OverlayKind.COUNTDOWN -> CountdownOverlay(engine)
            OverlayKind.PAUSE -> PauseOverlay(engine)
            OverlayKind.GAME_OVER -> GameOverOverlay(engine)
            OverlayKind.NONE -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MenuOverlay(engine: GameEngine) {
    var showHelp by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        val titleSize = (maxWidth.value / 4.6f).coerceAtMost(92f).sp

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp)
                .graphicsLayer { rotationZ = -5f }
        ) {
            DroppingTitle(text = "DEFENSA", startDelay = 150, color = Hueso, fontSize = titleSize)
            DroppingTitle(text = "ESPACIAL", startDelay = 560, color = Mostaza, fontSize = titleSize)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Esquiva los meteoritos y sobrevive\ntodo el tiempo que puedas.",
                style = MaterialTheme.typography.bodyLarge,
                color = Hueso
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = showHelp,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(tween(260)) + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Noche.copy(alpha = 0.92f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelpLine("Arrastra el dedo para mover la nave.")
                    HelpLine("La nave dispara sola y los meteoritos grandes se parten.")
                    HelpLine("Destruye meteoritos para cargar el pulso y limpia la pantalla.")
                }
            }
            if (engine.bestScore > 0) {
                Text(
                    text = "Tu récord: ${engine.bestScore} puntos",
                    style = MaterialTheme.typography.titleLarge,
                    color = Hueso
                )
            }
            PosterButton(
                text = "Iniciar misión",
                background = Mostaza,
                content = Noche,
                pulsing = true,
                onClick = engine::startMission
            )
            PosterButton(
                text = if (showHelp) "Ocultar ayuda" else "Cómo jugar",
                background = Noche,
                content = Hueso,
                onClick = { showHelp = !showHelp }
            )
        }
    }
}

@Composable
private fun HelpLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = Hueso)
}

// Las letras del título caen una por una con rebote
@Composable
private fun DroppingTitle(text: String, startDelay: Int, color: Color, fontSize: TextUnit) {
    Row {
        text.forEachIndexed { index, letter ->
            val drop = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                delay(startDelay + index * 55L)
                drop.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 240f))
            }
            Text(
                text = letter.toString(),
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                    fontWeight = MaterialTheme.typography.displayLarge.fontWeight,
                    fontSize = fontSize,
                    lineHeight = fontSize
                ),
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationY = -drop.value * 420f
                    rotationZ = drop.value * -28f
                    alpha = (1f - drop.value * 1.4f).coerceIn(0f, 1f)
                }
            )
        }
    }
}

@Composable
private fun CountdownOverlay(engine: GameEngine) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = engine.countdown,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.5f, stiffness = 300f), initialScale = 2.4f) +
                    fadeIn(tween(160))) togetherWith
                    (scaleOut(tween(220), targetScale = 0.4f) + fadeOut(tween(220)))
            },
            label = "cuentaRegresiva"
        ) { value ->
            Text(
                text = if (value == 0) "¡Ya!" else value.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 132.sp, lineHeight = 136.sp),
                color = if (value == 0) Mostaza else Hueso
            )
        }
    }
}

@Composable
private fun PauseOverlay(engine: GameEngine) {
    OverlayPanel {
        Text(
            text = "Pausa",
            style = MaterialTheme.typography.displayLarge,
            color = Hueso
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${formatTime(engine.secondsSurvived)} en vuelo y ${engine.score} puntos",
            style = MaterialTheme.typography.bodyLarge,
            color = Hueso
        )
        Spacer(Modifier.height(30.dp))
        PosterButton("Continuar", Mostaza, Noche, onClick = engine::resume)
        Spacer(Modifier.height(12.dp))
        PosterButton("Reiniciar misión", Hueso, Noche, onClick = engine::startMission)
        Spacer(Modifier.height(12.dp))
        PosterButton("Volver al menú", Noche, Hueso, onClick = engine::backToMenu)
    }
}

@Composable
private fun GameOverOverlay(engine: GameEngine) {
    // Los resultados suben desde cero al aparecer la pantalla
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val shownScore by animateIntAsState(
        targetValue = if (started) engine.score else 0,
        animationSpec = tween(1400, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "puntajeFinal"
    )
    val shownMeteors by animateIntAsState(
        targetValue = if (started) engine.meteorsDestroyed else 0,
        animationSpec = tween(1100, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "meteoritosFinales"
    )
    val recordState = remember {
        MutableTransitionState(false).apply { targetState = engine.isNewRecord }
    }

    OverlayPanel {
        Text(
            text = "Nave\ndestruida",
            style = MaterialTheme.typography.displayLarge,
            color = Bermellon
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = shownScore.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = Hueso
        )
        Text(text = "puntos", style = MaterialTheme.typography.titleLarge, color = Hueso)
        AnimatedVisibility(
            visibleState = recordState,
            enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 260f)) + fadeIn()
        ) {
            Text(
                text = "¡Nuevo récord!",
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(Mostaza)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Noche
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sobreviviste ${formatTime(engine.secondsSurvived)} y destruiste $shownMeteors meteoritos",
            style = MaterialTheme.typography.bodyLarge,
            color = Hueso
        )
        Spacer(Modifier.height(28.dp))
        PosterButton("Reintentar misión", Mostaza, Noche, pulsing = true, onClick = engine::startMission)
        Spacer(Modifier.height(12.dp))
        PosterButton("Volver al menú", Noche, Hueso, onClick = engine::backToMenu)
    }
}

@Composable
private fun OverlayPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Noche.copy(alpha = 0.72f))
            .pointerInput(Unit) { }
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
}

@Composable
private fun PosterButton(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "presionBoton"
    )
    val beat by rememberInfiniteTransition(label = "botonLatido").animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(760, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaBoton"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .graphicsLayer {
                val scale = pressScale * if (pulsing) beat else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(PosterShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = content)
    }
}
