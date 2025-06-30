package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.StopViewTheme
import com.aymanhki.peektransit.utils.FontUtils

@Composable
fun PreviewContent(
    theme: StopViewTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (theme) {
                    StopViewTheme.MODERN -> MaterialTheme.colorScheme.surface
                    StopViewTheme.CLASSIC -> Color.Black
                }
            ),
            //.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        val previewFontSize = 14.sp

        // First route row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "671",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = "Prairie Point",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PeekTransitConstants.LATE_STATUS_TEXT,
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.error
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = "1 ${PeekTransitConstants.MINUTES_REMAINING_TEXT}",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
        }
        
        // Second route row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "B",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = "Downtown",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PeekTransitConstants.EARLY_STATUS_TEXT,
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.primary
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = "11:15 ${PeekTransitConstants.GLOBAL_AM_TEXT}",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
        }
        
        // Third route row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "47",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
                
                Text(
                    text = "U of M",
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (theme) {
                        StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    },
                    color = when (theme) {
                        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                    }
                )
            }
            
            Text(
                text = PeekTransitConstants.CANCELLED_STATUS_TEXT,
                fontSize = previewFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = when (theme) {
                    StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                    StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                },
                color = when (theme) {
                    StopViewTheme.MODERN -> MaterialTheme.colorScheme.error
                    StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                }
            )
        }
    }
} 