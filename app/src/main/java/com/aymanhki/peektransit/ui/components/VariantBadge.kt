package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.data.models.Variant

@Composable
fun VariantBadge(
    variant: Variant,
    showFullVariantKey: Boolean = false,
    showVariantName: Boolean = false,
    modifier: Modifier = Modifier
) {
    val variantNumber = if (showFullVariantKey) {
        variant.key
    } else {
        variant.key.split("-").firstOrNull() ?: variant.key
    }
    
    val finalTextToShow = if (showVariantName) {
        "$variantNumber - ${variant.name}"
    } else {
        variantNumber
    }
    
    val backgroundColor = parseColorString(variant.backgroundColor) ?: MaterialTheme.colorScheme.primary
    val textColor = parseColorString(variant.textColor) ?: MaterialTheme.colorScheme.onPrimary
    val borderColor = parseColorString(variant.borderColor) ?: backgroundColor
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = finalTextToShow,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

private fun parseColorString(colorString: String?): Color? {
    return try {
        if (colorString.isNullOrBlank()) return null
        
        val cleanColor = colorString.trim()
        when {
            cleanColor.startsWith("#") && cleanColor.length == 7 -> {
                val colorInt = cleanColor.substring(1).toLong(16)
                Color(
                    red = ((colorInt shr 16) and 0xFF) / 255f,
                    green = ((colorInt shr 8) and 0xFF) / 255f,
                    blue = (colorInt and 0xFF) / 255f,
                    alpha = 1f
                )
            }
            cleanColor.startsWith("#") && cleanColor.length == 9 -> {
                val colorInt = cleanColor.substring(1).toLong(16)
                Color(
                    alpha = ((colorInt shr 24) and 0xFF) / 255f,
                    red = ((colorInt shr 16) and 0xFF) / 255f,
                    green = ((colorInt shr 8) and 0xFF) / 255f,
                    blue = (colorInt and 0xFF) / 255f
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}