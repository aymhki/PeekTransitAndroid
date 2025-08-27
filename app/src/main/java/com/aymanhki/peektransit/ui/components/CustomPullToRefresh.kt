package com.aymanhki.peektransit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch



@Composable
fun CustomPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTriggerDistance: Dp = 120.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val refreshTriggerPx = with(density) { refreshTriggerDistance.toPx() }
    
    val pullDistance = remember { Animatable(0f) }
    val refreshTriggered = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && refreshTriggered.value) {
            refreshTriggered.value = false
            pullDistance.animateTo(0f, animationSpec = tween(300))
        }
    }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0 && pullDistance.value > 0f && !isRefreshing) {
                    scope.launch {
                        val newDistance = (pullDistance.value + available.y).coerceAtLeast(0f)
                        pullDistance.snapTo(newDistance)
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0 && !isRefreshing) {
                    scope.launch {
                        val dragFactor = 0.5f
                        val newDistance = pullDistance.value + (available.y * dragFactor)
                        pullDistance.snapTo(newDistance.coerceAtLeast(0f))
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance.value > 0f && !isRefreshing) {
                    if (pullDistance.value >= refreshTriggerPx) {
                        refreshTriggered.value = true
                        onRefresh()
                        pullDistance.animateTo(refreshTriggerPx * 0.6f, animationSpec = tween(200))
                    } else {
                        pullDistance.animateTo(0f, animationSpec = tween(200))
                    }
                }
                return Velocity.Zero
            }
        }
    }
    
    Box(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = pullDistance.value
                }
        ) {
            content()
        }
        
        PullRefreshIndicator(
            isRefreshing = isRefreshing,
            pullDistance = pullDistance.value,
            refreshTriggerDistance = refreshTriggerPx,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PullRefreshIndicator(
    isRefreshing: Boolean,
    pullDistance: Float,
    refreshTriggerDistance: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    val progress = (pullDistance / refreshTriggerDistance).coerceIn(0f, 1f)
    val scale by animateFloatAsState(
        targetValue = if (pullDistance > 0f) (0.6f + (progress * 0.4f)) else 0f,
        animationSpec = tween(150),
        label = "indicator_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (pullDistance > 0f) progress.coerceIn(0.3f, 1f) else 0f,
        animationSpec = tween(150),
        label = "indicator_alpha"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) 
            tween(1000, easing = androidx.compose.animation.core.LinearEasing) 
        else tween(200),
        label = "indicator_rotation"
    )
    
    var infiniteRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (isRefreshing) {
                infiniteRotation += 360f
                kotlinx.coroutines.delay(1000)
            }
        } else {
            infiniteRotation = 0f
        }
    }
    
    Card(
        modifier = modifier
            .offset(y = with(density) { (pullDistance * 0.5f).toDp() })
            .size(40.dp)
            .alpha(alpha)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (isRefreshing) infiniteRotation else rotation
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pullDistance > 0f) 6.dp else 0.dp
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
} 