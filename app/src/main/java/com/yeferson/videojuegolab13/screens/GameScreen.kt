package com.yeferson.videojuegolab13.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yeferson.videojuegolab13.ui.theme.Hueso
import com.yeferson.videojuegolab13.ui.theme.Noche

@Composable
fun GameScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Noche),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Defensa Espacial",
            style = MaterialTheme.typography.headlineLarge,
            color = Hueso
        )
    }
}
