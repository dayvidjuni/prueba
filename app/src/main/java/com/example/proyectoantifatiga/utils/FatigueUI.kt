package com.example.proyectoantifatiga.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FatigueUI(
    showFatigueMessage: MutableState<Boolean>,
    showYawnMessage: MutableState<Boolean>,
    previewView: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        previewView()

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showFatigueMessage.value) {
                Text(
                    text = "😴 Fatiga detectada",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            if (showYawnMessage.value) {
                Text(
                    text = "🥱 Bostezo detectado",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
