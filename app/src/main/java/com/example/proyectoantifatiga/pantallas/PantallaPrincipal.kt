package com.example.proyectoantifatiga.pantallas

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.proyectoantifatiga.ui.screen.BlackScreenWithDetection

@Composable
fun PantallaPrincipal() {

    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }//para conseder permiso a la camara, inicializa como falso.

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }
    LaunchedEffect(Unit) {

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                permissionGranted = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    if (permissionGranted) {
        BlackScreenWithDetection()
    }
}