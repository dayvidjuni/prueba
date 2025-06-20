package com.example.proyectoantifatiga.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
private fun FullScreenFatigueWarning() {
    // Animación de pulso
    val scale by animateFloatAsState(
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .scale(scale),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "TOMA UN",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "DESCANSO",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun FatigueUI(
    showFatigueMessage: MutableState<Boolean>,
    showYawnMessage: MutableState<Boolean>,
    previewView: @Composable () -> Unit,
    // Nuevos parámetros para métricas avanzadas
    fatigueLevel: MutableState<Float> = mutableStateOf(0f),
    distanceWarning: MutableState<Boolean> = mutableStateOf(false),
    blinkRate: MutableState<Int> = mutableStateOf(0),
    eyeAspectRatio: MutableState<Float> = mutableStateOf(0.25f),
    faceDetected: MutableState<Boolean> = mutableStateOf(false)
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Vista de la cámara
        previewView()

        // Overlay con información de estado
        StatusOverlay(
            showFatigueMessage = showFatigueMessage,
            showYawnMessage = showYawnMessage,
            fatigueLevel = fatigueLevel,
            distanceWarning = distanceWarning,
            blinkRate = blinkRate,
            eyeAspectRatio = eyeAspectRatio,
            faceDetected = faceDetected
        )

        // Mensaje de fatiga en pantalla completa
        val showFatigueValue by remember { showFatigueMessage }
        if (showFatigueValue) {
            FullScreenFatigueWarning()
        }
    }
}

@Composable
private fun StatusOverlay(
    showFatigueMessage: MutableState<Boolean>,
    showYawnMessage: MutableState<Boolean>,
    fatigueLevel: MutableState<Float>,
    distanceWarning: MutableState<Boolean>,
    blinkRate: MutableState<Int>,
    eyeAspectRatio: MutableState<Float>,
    faceDetected: MutableState<Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Advertencia de distancia - usando by remember para recomposición
        val distanceWarningValue by remember { distanceWarning }
        AnimatedVisibility(
            visible = distanceWarningValue,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            EnhancedAlertCard(
                title = "Ajusta tu posición",
                message = "Acércate más a la cámara para mejor detección",
                icon = Icons.Default.CameraAlt,
                color = Color(0xFFFF9800), // Orange
                urgent = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mensaje principal de fatiga - usando by remember para recomposición
        val showFatigueValue by remember { showFatigueMessage }
        val fatigueLevelValue by remember { fatigueLevel }
        AnimatedVisibility(
            visible = showFatigueValue,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            EnhancedAlertCard(
                title = "¡FATIGA DETECTADA!",
                message = "Considera tomar un descanso",
                icon = Icons.Default.Warning,
                color = when {
                    fatigueLevelValue > 0.8f -> Color(0xFFD32F2F) // Rojo intenso
                    fatigueLevelValue > 0.6f -> Color(0xFFFF5722) // Naranja rojo
                    else -> Color(0xFFFF9800) // Naranja
                },
                urgent = fatigueLevelValue > 0.7f
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mensaje de bostezo - usando by remember para recomposición
        val showYawnValue by remember { showYawnMessage }
        AnimatedVisibility(
            visible = showYawnValue,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            EnhancedAlertCard(
                title = "¡BOSTEZO DETECTADO!",
                message = "Señal clara de somnolencia - considera descansar",
                icon = Icons.Default.Face,
                color = Color(0xFFFFC107), // Amarillo
                urgent = true // Cambiado a true para más visibilidad
            )
        }

        // Barra de progreso de fatiga siempre visible - usando by remember para recomposición
        val faceDetectedValue by remember { faceDetected }
        val blinkRateValue by remember { blinkRate }
        if (faceDetectedValue && !showFatigueValue) {
            Spacer(modifier = Modifier.height(16.dp))
            FatigueProgressBar(
                fatigueLevel = fatigueLevelValue,
                blinkRate = blinkRateValue
            )
        }
    }
}

@Composable
private fun EnhancedAlertCard(
    title: String,
    message: String,
    icon: ImageVector,
    color: Color,
    urgent: Boolean
) {
    // Animación de pulso para alertas urgentes
    val scale by animateFloatAsState(
        targetValue = if (urgent) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}



@Composable
private fun FatigueProgressBar(
    fatigueLevel: Float,
    blinkRate: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = fatigueLevel,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nivel de Fatiga",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Green,
                                    Color.Yellow,
                                    Color.Blue,
                                    Color.Red
                                ),
                                startX = 0f,
                                endX = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indicador de frecuencia de parpadeo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Parpadeos: $blinkRate/min",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text = when {
                        blinkRate < 10 -> "Muy bajo"
                        blinkRate < 15 -> "Bajo"
                        blinkRate < 25 -> "Normal"
                        else -> "Alto"
                    },
                    color = when {
                        blinkRate < 10 -> Color.Red
                        blinkRate < 15 -> Color.Blue
                        blinkRate < 25 -> Color.Green
                        else -> Color.Yellow
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}