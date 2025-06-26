package com.example.proyectoantifatiga.service

import android.content.Context
import android.media.MediaPlayer
import com.example.proyectoantifatiga.R

object ServicioFatiga {
    private var mediaPlayer: MediaPlayer? = null

    fun iniciarAlarma(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.alarma)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    fun detenerAlarma() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
