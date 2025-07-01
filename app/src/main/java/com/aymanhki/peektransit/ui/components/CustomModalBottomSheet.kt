package com.aymanhki.peektransit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs


@Composable
fun CustomModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable (() -> Unit)? = { CustomDragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "drag_offset"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val backdropAlpha = (0.5f - (animatedDragOffset / 1000f)).coerceIn(0.1f, 0.5f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = backdropAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isVisible = false
                        onDismissRequest()
                    }
            )
            
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                                 Surface(
                     modifier = modifier
                         .fillMaxWidth()
                         .wrapContentHeight()
                         .offset(y = with(density) { animatedDragOffset.toDp() })
                         .pointerInput(Unit) {
                             detectDragGestures(
                                 onDrag = { _, dragAmount ->
                                     val newOffset = dragOffset + dragAmount.y
                                     dragOffset = newOffset.coerceAtLeast(0f)
                                 },
                                 onDragEnd = {
                                     if (dragOffset > 120f) {
                                         isVisible = false
                                         onDismissRequest()
                                     } else {
                                         dragOffset = 0f
                                     }
                                 }
                             )
                         },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        dragHandle?.let { handle ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                handle()
                            }
                        }
                        
                        content()
                    }
                }
            }
        }
    }
}


@Composable
fun CustomDragHandle() {
    Surface(
        modifier = Modifier
            .width(32.dp)
            .height(4.dp),
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {}
} 