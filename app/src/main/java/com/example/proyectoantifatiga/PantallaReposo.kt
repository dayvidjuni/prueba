package com.example.proyectoantifatiga
/*
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

class PantallaReposo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Oculta la barra de estado y atenúa la pantalla
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContent { PantallaOscuraConHora() }
    }
}

@Composable
fun PantallaOscuraConHora() {
    val tiempoActual = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            tiempoActual.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000L) // Actualiza la hora cada segundo
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            text = tiempoActual.value,
            color = Color.White,
            fontSize = 48.sp,
            modifier = androidx.compose.ui.Modifier.align(Alignment.Center)
        )
    }
}*/
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

class PantallaReposo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Oculta la barra de estado y atenúa la pantalla
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        //window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContent { PantallaNegraConHora() }
    }
}

@Composable
fun PantallaNegraConHora() {
    val tiempoActual = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            tiempoActual.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000L) // 🔹 La hora se actualiza cada segundo
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            text = tiempoActual.value,
            color = Color.White,
            fontSize = 48.sp,
            modifier = androidx.compose.ui.Modifier.align(Alignment.Center)
        )
    }
}