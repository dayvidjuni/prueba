package com.example.proyectoantifatiga.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

@Composable
fun FatigueUI(
    showFatigueMessage: MutableState<Boolean>,
    showYawnMessage: MutableState<Boolean>,
    previewView: @Composable () -> Unit,
    eyeLandmarks: List<NormalizedLandmark>? = null,
    fatigueLevel: MutableState<Float> = mutableStateOf(0f),
    distanceWarning: MutableState<Boolean> = mutableStateOf(false),
    blinkRate: MutableState<Int> = mutableStateOf(0),
    eyeAspectRatio: MutableState<Float> = mutableStateOf(0.25f),
    faceDetected: MutableState<Boolean> = mutableStateOf(false)
) {
    Box(modifier = Modifier.fillMaxSize()) {
        previewView()

        // Dibujar puntos de ojos
        eyeLandmarks?.let { landmarks ->
            val leftEye = listOf(33, 133, 160, 158, 159, 157, 173)
            val rightEye = listOf(362, 263, 387, 385, 386, 384, 398)
            val eyeIndices = leftEye + rightEye

            Canvas(modifier = Modifier.fillMaxSize()) {
                eyeIndices.forEach { i ->
                    if (i < landmarks.size) {
                        val lm = landmarks[i]
                        val x = (1f - lm.x()) * size.width
                        val y = lm.y() * size.height
                        drawCircle(Color.Cyan, radius = 6f, center = Offset(x, y))
                    }
                }
            }
        }

        val fatigue = fatigueLevel.value
        val blink = blinkRate.value
        val showFatigue by remember { showFatigueMessage }
        val showYawn by remember { showYawnMessage }
        val warnDistance by remember { distanceWarning }
        val faceOk by remember { faceDetected }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (warnDistance) {
                AlertCard(
                    title = "Ajusta tu posición",
                    message = "Acércate más a la cámara para mejor detección",
                    icon = Icons.Default.CameraAlt,
                    color = Color(0xFFFF9800),
                    urgent = false
                )
            }

            AnimatedVisibility(visible = showFatigue, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                AlertCard(
                    title = "¡FATIGA DETECTADA!",
                    message = "Considera tomar un descanso",
                    icon = Icons.Default.Warning,
                    color = when {
                        fatigue > 0.8f -> Color(0xFFD32F2F)
                        fatigue > 0.6f -> Color(0xFFFF5722)
                        else -> Color(0xFFFF9800)
                    },
                    urgent = fatigue > 0.7f
                )
            }

            AnimatedVisibility(visible = showYawn, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                AlertCard(
                    title = "¡BOSTEZO DETECTADO!",
                    message = "Señal clara de somnolencia - considera descansar",
                    icon = Icons.Default.Face,
                    color = Color(0xFFFFC107),
                    urgent = true
                )
            }

            if (faceOk && !showFatigue) {
                Spacer(modifier = Modifier.height(16.dp))
                val animProgress by animateFloatAsState(
                    targetValue = fatigue,
                    animationSpec = tween(300),
                    label = "progress"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nivel de Fatiga", color = Color.White, fontSize = 14.sp)
                            Text("${(animProgress * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
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
                                    .fillMaxWidth(animProgress)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Green, Color.Yellow, Color.Blue, Color.Red
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Parpadeos: $blink/min", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                text = when {
                                    blink < 10 -> "Muy bajo"
                                    blink < 15 -> "Bajo"
                                    blink < 25 -> "Normal"
                                    else -> "Alto"
                                },
                                color = when {
                                    blink < 10 -> Color.Red
                                    blink < 15 -> Color.Blue
                                    blink < 25 -> Color.Green
                                    else -> Color.Yellow
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        if (showFatigue) {
            val scale by animateFloatAsState(
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fatigue_warning"
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .scale(scale),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(16.dp),
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
                        Text("TOMA UN", color = Color.White, fontSize = 42.sp)
                        Text("DESCANSO", color = Color.White, fontSize = 42.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    title: String,
    message: String,
    icon: ImageVector,
    color: Color,
    urgent: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (urgent) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.95f)),
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
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = message, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 18.sp)
            }
        }
    }
}
