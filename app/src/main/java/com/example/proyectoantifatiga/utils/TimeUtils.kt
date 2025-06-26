package com.example.proyectoantifatiga.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun getCurrentTime(): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return format.format(Date())
    }
}
