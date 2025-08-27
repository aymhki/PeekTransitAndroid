package com.aymanhki.peektransit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.utils.BannerType
import com.aymanhki.peektransit.viewmodel.MainViewModel


@Composable
fun BannerView(
    activeBanner: BannerType?,
    mainViewModel: MainViewModel,
    isMapScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val isVisible = activeBanner != null
    var bannerContent by remember { mutableStateOf(activeBanner) }

    LaunchedEffect(activeBanner) {
        if (activeBanner != null) {
            bannerContent = activeBanner
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { if (isMapScreen) -it else it },
            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { if (isMapScreen) -it else it },
            animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier
    ) {
        bannerContent?.let { banner ->
            val (icon, text, backgroundColor) = when (banner) {
                BannerType.UPDATE -> Triple(
                    Icons.Default.ArrowCircleDown,
                    "Update Available",
                    MaterialTheme.colorScheme.primary
                )
                BannerType.TIP -> Triple(
                    Icons.Default.Favorite,
                    "Support Development",
                    Color(0xFFE91E63)
                )
                BannerType.RATE -> Triple(
                    Icons.Default.Stars,
                    "Rate Peek Transit",
                    MaterialTheme.colorScheme.primary
                )
            }

            Card(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clickable {
                        when (banner) {
                            BannerType.UPDATE -> {
                                mainViewModel.updateBannerWasTapped()
                            }
                            BannerType.TIP -> {
                                mainViewModel.tipBannerWasTapped()
                            }
                            BannerType.RATE -> {
                                mainViewModel.rateAppBannerWasTapped()
                            }
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
