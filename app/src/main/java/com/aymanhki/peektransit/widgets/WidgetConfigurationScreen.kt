package com.aymanhki.peektransit.widgets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.ui.theme.AccentBlue
import com.aymanhki.peektransit.ui.theme.PeekTransitTheme


@Composable
fun WidgetConfigurationScreen(
    title: String,
    widgetsToShow: List<WidgetModel>,
    onWidgetSelected: (WidgetModel) -> Unit
) {
    PeekTransitTheme {
        Scaffold(
            topBar = {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 24.dp,
                        bottom = 24.dp,
                        end = 16.dp
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.padding(paddingValues)
            ) {
                items(widgetsToShow.size) { index ->
                    val widget = widgetsToShow[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = { onWidgetSelected(widget) }
                    ) {
                        Text(
                            text = widget.widgetData["name"] as? String ?: "Unnamed Widget",
                            modifier = Modifier.padding(16.dp),
                            maxLines = 5,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )

                        if (widget.widgetData["isClosestStop"] as? Boolean == true) {
                            Text(
                                text = "Requires location access to show closest stop",
                                modifier = Modifier.padding(16.dp),
                                color = AccentBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
