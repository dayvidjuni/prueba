package com.example.proyectoantifatiga

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ServicioFatiga : Service() {
    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        val notificacion = NotificationCompat.Builder(this, "CANAL_FATIGA")
            .setContentTitle("Monitoreo Activo")
            .setContentText("La aplicación sigue funcionando en segundo plano")
            .setSmallIcon(R.drawable.ic_notificacion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notificacion)
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "CANAL_FATIGA",
                "Monitoreo de Fatiga",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}