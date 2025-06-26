package com.example.proyectoantifatiga.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun HeaderImage(
    logoResourceId: Int, // ID del recurso de imagen (ej: R.drawable.logo)
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = logoResourceId),
        contentDescription = "Logo de la aplicación",
        modifier = modifier
            .size(125.dp) // Más grande
            .padding(8.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun HeaderImageSimple(
    logoResourceId: Int,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = logoResourceId),
        contentDescription = "Logo de la aplicación",
        modifier = modifier
            .size(100.dp) // Más grande
            .padding(4.dp),
        contentScale = ContentScale.Fit
    )
}