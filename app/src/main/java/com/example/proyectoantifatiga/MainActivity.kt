package com.example.proyectoantifatiga
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.proyectoantifatiga.pantallas.Permisocamara

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)//se recrea la actividad con el estado previamente guardado
        setContent {//define el contenido visual de la actividad
            Permisocamara()
        }
    }
}