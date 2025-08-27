package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.StopViewTheme

@Composable
fun ThemePreviewCard(
    theme: StopViewTheme,
    isSelected: Boolean,
    onThemeSelected: (StopViewTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onThemeSelected(theme) }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = when (theme) {
                StopViewTheme.MODERN -> MaterialTheme.colorScheme.surface
                StopViewTheme.CLASSIC -> Color.Black
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = theme.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onSurface
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = theme.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onSurface
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
            
            PreviewContent(theme = theme)
        }
    }
} 